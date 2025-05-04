import co.touchlab.kermit.Severity
import com.mineinabyss.geary.helpers.entity
import com.mineinabyss.geary.modules.ArchetypeEngineModule
import com.mineinabyss.geary.modules.GearySetup
import com.mineinabyss.geary.modules.geary
import com.mineinabyss.geary.systems.query.query
import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolContext
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
import de.fabmax.kool.util.RenderLoop
import de.fabmax.kool.util.Time
import kotlinx.coroutines.Dispatchers
import kotlin.random.Random

data class Position(var x: Float, var y: Float, var z: Float)
data class Velocity(val x: Float, val y: Float, val z: Float)

fun GearySetup.tickOnUpdate(ctx: KoolContext) {
    ctx.onRender += { geary.tick() }
}

fun main() = KoolApplication {
    val world = geary(
        ArchetypeEngineModule(
            engineThread = { Dispatchers.RenderLoop })
    ) {
        loggerSeverity(Severity.Info)
        tickOnUpdate(ctx)
    }.start()

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
            generate {
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
            repeat(100000) {
                entity {
                    fun randomCoord() = (-10..10).random().toFloat()
                    fun randomVelocity() = Random.nextFloat()
                    set(Position(randomCoord(), randomCoord(), randomCoord()))
                    set(Velocity(randomVelocity(), randomVelocity(), randomVelocity()))
                }
            }

            system(query<Position>()).execOnAll {
                mesh.instances?.clear()
                mesh.instances?.addInstances(count()) { buffer ->
                    forEach { (position) ->
                        MutableMat4f().translate(position.x, position.y, position.z).putTo(buffer)
                    }
                }
            }

            // system to update positions based on velocities
            system(query<Position, Velocity>()).exec { (pos, vel) ->
                val dT = Time.deltaT
                pos.x += vel.x * dT
                pos.y += vel.y * dT
                pos.z += vel.z * dT
            }
        }

        addNode(mesh)
    }
}
