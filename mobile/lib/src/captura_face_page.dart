import 'package:camera/camera.dart';
import 'package:flutter/material.dart';

/// Tela de captura facial (camera frontal). Retorna o caminho da imagem via Navigator.pop.
class CapturaFacePage extends StatefulWidget {
  const CapturaFacePage({super.key});

  @override
  State<CapturaFacePage> createState() => _CapturaFacePageState();
}

class _CapturaFacePageState extends State<CapturaFacePage> {
  CameraController? _controller;
  bool _pronto = false;
  String? _erro;

  @override
  void initState() {
    super.initState();
    _iniciar();
  }

  Future<void> _iniciar() async {
    try {
      final cameras = await availableCameras();
      if (cameras.isEmpty) {
        setState(() => _erro = 'Nenhuma camera disponivel');
        return;
      }
      final frontal = cameras.firstWhere(
        (c) => c.lensDirection == CameraLensDirection.front,
        orElse: () => cameras.first,
      );
      final controller = CameraController(frontal, ResolutionPreset.medium, enableAudio: false);
      await controller.initialize();
      if (!mounted) return;
      setState(() {
        _controller = controller;
        _pronto = true;
      });
    } catch (e) {
      if (mounted) setState(() => _erro = 'Falha ao abrir camera: $e');
    }
  }

  Future<void> _capturar() async {
    final c = _controller;
    if (c == null || !c.value.isInitialized) return;
    final XFile foto = await c.takePicture();
    if (!mounted) return;
    Navigator.of(context).pop(foto.path);
  }

  @override
  void dispose() {
    _controller?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Captura facial')),
      body: _erro != null
          ? Center(child: Text(_erro!, style: const TextStyle(color: Colors.red)))
          : (_pronto && _controller != null
              ? Column(
                  children: [
                    Expanded(child: CameraPreview(_controller!)),
                    Padding(
                      padding: const EdgeInsets.all(16),
                      child: ElevatedButton.icon(
                        onPressed: _capturar,
                        icon: const Icon(Icons.camera_alt),
                        label: const Text('Capturar'),
                      ),
                    ),
                  ],
                )
              : const Center(child: CircularProgressIndicator())),
    );
  }
}
