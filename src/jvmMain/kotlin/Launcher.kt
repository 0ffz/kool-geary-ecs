import co.touchlab.kermit.Severity
import com.mineinabyss.geary.helpers.entity
import com.mineinabyss.geary.modules.ArchetypeEngineModule
import com.mineinabyss.geary.modules.Geary
import com.mineinabyss.geary.modules.geary
import com.mineinabyss.geary.systems.query.Query
import com.mineinabyss.geary.systems.query.query
import de.fabmax.kool.KoolApplication
import de.fabmax.kool.addScene
import de.fabmax.kool.math.MutableMat4f
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.deg
import de.fabmax.kool.modules.ksl.KslPbrShader
import de.fabmax.kool.pipeline.Attribute
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.MeshInstanceList
import de.fabmax.kool.scene.defaultOrbitCamera
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.Time
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

data class Position(val x: Float, val y: Float, val z: Float)
data class Velocity(val x: Float, val y: Float, val z: Float)

fun main() = KoolApplication {
    Geary.mutableConfig.minSeverity = Severity.Info
    val world = geary(ArchetypeEngineModule()).start()
    addScene {
        defaultOrbitCamera()
        lighting.singleDirectionalLight {
            setup(Vec3f(-1f, -1f, -1f))
            setColor(Color.WHITE, 5f)
        }
        val mesh = Mesh(
            Attribute.POSITIONS,
            Attribute.NORMALS,
            Attribute.COLORS,
            instances = MeshInstanceList(listOf(Attribute.INSTANCE_MODEL_MAT), initialSize = 10)
        ).apply {
            this.generate {
                cube {
                    colored()
                }
                shader = KslPbrShader {
                    vertices { isInstanced = true }
                    color { vertexColor() }
                    metallic(0f)
                    roughness(0.25f)
                }
                onUpdate {
                    transform.rotate(45f.deg * Time.deltaT, Vec3f.X_AXIS)
                }
            }
        }
        with(world) {
            repeat(10) {
                entity {
                    fun randomCoord() = (0..10).random().toFloat()
                    fun randomVelocity() = Random.nextFloat()
                    set(Position(randomCoord(), randomCoord(), randomCoord()))
                    set(Velocity(randomVelocity(), randomVelocity(), randomVelocity()))
                }
            }

            //TODO DEFAULT TICK RATE
            system(query<Position>()).every(20.milliseconds).execOnAll {
                mesh.instances?.clear()
                mesh.instances?.addInstances(count()) { buffer ->
                    forEach { (position) ->
                        MutableMat4f().translate(position.x, position.y, position.z).putTo(buffer)
                    }
                }
            }


            class PositionQuery : Query(world) {
                var position by get<Position>()
                val velocity by get<Velocity>()
            }

            // system to update positions based on velocities
            system(PositionQuery()).every(20.milliseconds).exec {
                val vel = it.velocity
                val pos = it.position
                it.position = Position(pos.x + vel.x, pos.y + vel.y, pos.z + vel.z)
            }
        }

        addNode(mesh)
    }
}
