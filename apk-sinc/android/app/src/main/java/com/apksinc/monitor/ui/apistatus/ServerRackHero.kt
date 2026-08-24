package com.apksinc.monitor.ui.apistatus

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes

/**
 * Renderiza o modelo real do rack de servidor (assets/models/server_rack.glb,
 * "Server Rack" by Spellkaze, licenca CC Attribution) via SceneView/Filament,
 * girando devagar no eixo Y. Decorativa, usada so na tela de Status da API.
 */
@Composable
fun ServerRackHero(isConnected: Boolean, modifier: Modifier = Modifier) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Position(z = 5.0f)
    }

    val modelNode = ModelNode(
        modelInstance = modelLoader.createModelInstance(assetFileLocation = "models/server_rack.glb"),
        scaleToUnits = 2.2f,
    )
    val childNodes = rememberNodes { add(modelNode) }

    LaunchedEffect(modelNode) {
        var angle = 0f
        while (true) {
            kotlinx.coroutines.delay(16)
            angle = (angle + 0.4f) % 360f
            modelNode.rotation = modelNode.rotation.copy(y = angle)
        }
    }

    Scene(
        modifier = modifier.fillMaxWidth().height(150.dp),
        engine = engine,
        modelLoader = modelLoader,
        cameraNode = cameraNode,
        childNodes = childNodes,
    )
}
