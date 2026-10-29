import 'dart:io';

import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:image_picker/image_picker.dart';

import '../api.dart';
import '../models.dart';

/// Capture a photo, tag it with GPS, submit, and see the AI severity result.
class ReportScreen extends StatefulWidget {
  final Future<void> Function() onSubmitted;
  final Future<void> Function() onSessionExpired;
  const ReportScreen({super.key, required this.onSubmitted, required this.onSessionExpired});

  @override
  State<ReportScreen> createState() => _ReportScreenState();
}

class _ReportScreenState extends State<ReportScreen> {
  final _picker = ImagePicker();
  File? _photo;
  Position? _position;
  String? _locationError;
  bool _locating = false;
  bool _submitting = false;
  Complaint? _result;

  @override
  void initState() {
    super.initState();
    _locate();
  }

  Future<void> _locate() async {
    setState(() {
      _locating = true;
      _locationError = null;
    });
    try {
      if (!await Geolocator.isLocationServiceEnabled()) {
        throw 'Location services are turned off. Please enable GPS.';
      }
      var permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
      }
      if (permission == LocationPermission.denied || permission == LocationPermission.deniedForever) {
        throw 'Location permission is required to tag the report with where the garbage is.';
      }
      final pos = await Geolocator.getCurrentPosition(
        locationSettings: const LocationSettings(accuracy: LocationAccuracy.high, timeLimit: Duration(seconds: 20)),
      );
      // The paper's anti-spoofing rule: refuse simulated coordinates.
      if (pos.isMocked) {
        throw 'Mock/simulated location detected. Turn it off to file a report.';
      }
      if (mounted) setState(() => _position = pos);
    } catch (e) {
      if (mounted) setState(() => _locationError = e.toString());
    } finally {
      if (mounted) setState(() => _locating = false);
    }
  }

  Future<void> _pick(ImageSource source) async {
    try {
      final x = await _picker.pickImage(source: source, imageQuality: 80, maxWidth: 1600);
      if (x != null) {
        setState(() {
          _photo = File(x.path);
          _result = null;
        });
        _locate(); // refresh GPS right at capture time
      }
    } catch (e) {
      _snack('Could not open the camera/gallery: $e');
    }
  }

  Future<void> _submit() async {
    if (_photo == null || _position == null) return;
    setState(() => _submitting = true);
    try {
      final c = await Api.submitReport(
        photo: _photo!,
        latitude: _position!.latitude,
        longitude: _position!.longitude,
      );
      if (!mounted) return;
      setState(() {
        _result = c;
        _photo = null;
      });
      await widget.onSubmitted();
    } on ApiException catch (e) {
      if (e.isUnauthorized) {
        await widget.onSessionExpired();
        return;
      }
      _snack(e.message);
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  void _snack(String msg) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final canSubmit = _photo != null && _position != null && !_submitting;

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        if (_result != null) _ResultCard(complaint: _result!),
        Card(
          clipBehavior: Clip.antiAlias,
          child: Column(
            children: [
              AspectRatio(
                aspectRatio: 4 / 3,
                child: _photo == null
                    ? Container(
                        color: scheme.surfaceContainerHighest,
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(Icons.photo_camera_outlined, size: 56, color: scheme.onSurfaceVariant),
                            const SizedBox(height: 8),
                            Text('Photograph the garbage', style: TextStyle(color: scheme.onSurfaceVariant)),
                          ],
                        ),
                      )
                    : Image.file(_photo!, fit: BoxFit.cover),
              ),
              Padding(
                padding: const EdgeInsets.all(12),
                child: Row(
                  children: [
                    Expanded(
                      child: FilledButton.icon(
                        onPressed: _submitting ? null : () => _pick(ImageSource.camera),
                        icon: const Icon(Icons.photo_camera),
                        label: const Text('Take photo'),
                      ),
                    ),
                    const SizedBox(width: 10),
                    OutlinedButton.icon(
                      onPressed: _submitting ? null : () => _pick(ImageSource.gallery),
                      icon: const Icon(Icons.photo_library_outlined),
                      label: const Text('Gallery'),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 8),
        Card(
          child: ListTile(
            leading: _locating
                ? const SizedBox(height: 24, width: 24, child: CircularProgressIndicator(strokeWidth: 2))
                : Icon(_position != null ? Icons.my_location : Icons.location_off,
                    color: _position != null ? scheme.primary : scheme.error),
            title: Text(_position != null
                ? '${_position!.latitude.toStringAsFixed(5)}, ${_position!.longitude.toStringAsFixed(5)}'
                : (_locating ? 'Getting your location…' : 'Location unavailable')),
            subtitle: Text(_locationError ??
                (_position != null ? 'Accuracy ±${_position!.accuracy.round()} m' : 'Needed to tag the report')),
            trailing: IconButton(onPressed: _locating ? null : _locate, icon: const Icon(Icons.refresh), tooltip: 'Refresh location'),
          ),
        ),
        const SizedBox(height: 8),
        FilledButton(
          onPressed: canSubmit ? _submit : null,
          style: FilledButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 16)),
          child: _submitting
              ? const Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    SizedBox(height: 18, width: 18, child: CircularProgressIndicator(strokeWidth: 2)),
                    SizedBox(width: 12),
                    Text('Analysing photo…'),
                  ],
                )
              : const Text('Submit report'),
        ),
        const SizedBox(height: 8),
        Text(
          'Your photo is analysed by AI to rate how severe the garbage is. Worse piles get cleaned sooner.',
          textAlign: TextAlign.center,
          style: TextStyle(color: scheme.onSurfaceVariant, fontSize: 12),
        ),
      ],
    );
  }
}

class _ResultCard extends StatelessWidget {
  final Complaint complaint;
  const _ResultCard({required this.complaint});

  @override
  Widget build(BuildContext context) {
    final c = complaint;
    return Card(
      color: Theme.of(context).colorScheme.primaryContainer,
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Row(
          children: [
            CircleAvatar(
              radius: 26,
              backgroundColor: severityColor(c.severity),
              child: Text('${c.severity}', style: const TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.bold)),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Report #${c.id} received', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                  const SizedBox(height: 2),
                  Text('${c.garbageType} · ${c.coveragePct.round()}% of the frame'),
                  Text('Target cleanup time: ${c.slaLabel}'),
                  if (c.needsReview) const Text('Flagged for manual review', style: TextStyle(fontStyle: FontStyle.italic)),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
