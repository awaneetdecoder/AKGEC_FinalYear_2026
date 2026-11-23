import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:geolocator/geolocator.dart';
import 'package:image_picker/image_picker.dart';

import '../api.dart';
import '../models.dart';
import '../widgets.dart';

/// Photo -> GPS -> submit. The AI severity result opens as a sheet.
class ReportScreen extends StatefulWidget {
  final Future<void> Function() onSubmitted;
  final Future<void> Function() onSessionExpired;
  final VoidCallback onViewReports;
  const ReportScreen({
    super.key,
    required this.onSubmitted,
    required this.onSessionExpired,
    required this.onViewReports,
  });

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
      // Anti-spoofing: refuse simulated coordinates.
      if (pos.isMocked) {
        throw 'A mock/simulated location was detected. Turn it off to file a report.';
      }
      if (mounted) setState(() => _position = pos);
    } catch (e) {
      if (mounted) {
        setState(() {
          _position = null;
          _locationError = e.toString();
        });
      }
    } finally {
      if (mounted) setState(() => _locating = false);
    }
  }

  Future<void> _pick(ImageSource source) async {
    try {
      final x = await _picker.pickImage(source: source, imageQuality: 80, maxWidth: 1600);
      if (x != null) {
        setState(() => _photo = File(x.path));
        _locate(); // refresh GPS right when the photo is taken
      }
    } catch (e) {
      if (mounted) showAppSnack(context, 'Could not open the camera or gallery: $e', error: true);
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
      HapticFeedback.mediumImpact();
      setState(() => _photo = null);
      await widget.onSubmitted();
      if (mounted) _showResult(c);
    } on ApiException catch (e) {
      if (e.isUnauthorized) {
        await widget.onSessionExpired();
        return;
      }
      if (mounted) showAppSnack(context, e.message, error: true);
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  void _showResult(Complaint c) {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(28))),
      builder: (sheetContext) => _ResultSheet(
        complaint: c,
        onDone: () => Navigator.of(sheetContext).pop(),
        onViewReports: () {
          Navigator.of(sheetContext).pop();
          widget.onViewReports();
        },
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // UI
  // ---------------------------------------------------------------------------

  /// How trustworthy the GPS fix is, in plain words.
  (String, Color) _accuracyInfo(ColorScheme scheme) {
    final a = _position?.accuracy ?? 999;
    if (a <= 15) return ('Excellent signal', const Color(0xFF2E9E5B));
    if (a <= 40) return ('Good signal', const Color(0xFF8BB832));
    return ('Weak signal, step into the open', const Color(0xFFE0A21B));
  }

  Widget _stepper(ColorScheme scheme) {
    final photoDone = _photo != null;
    final locDone = _position != null;
    final steps = <(String, bool)>[('Photo', photoDone), ('Location', locDone), ('Submit', false)];
    final activeIndex = !photoDone ? 0 : (!locDone ? 1 : 2);

    Widget node(int i) {
      final done = steps[i].$2;
      final active = i == activeIndex;
      final color = done || active ? scheme.primary : scheme.outlineVariant;
      return Column(
        children: [
          AnimatedContainer(
            duration: const Duration(milliseconds: 220),
            width: 30,
            height: 30,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: done ? scheme.primary : scheme.surface,
              shape: BoxShape.circle,
              border: Border.all(color: color, width: 2),
            ),
            child: done
                ? Icon(Icons.check, size: 17, color: scheme.onPrimary)
                : Text('${i + 1}', style: TextStyle(fontWeight: FontWeight.w800, fontSize: 13, color: color)),
          ),
          const SizedBox(height: 4),
          Text(steps[i].$1,
              style: TextStyle(
                  fontSize: 11.5,
                  fontWeight: active || done ? FontWeight.w700 : FontWeight.w500,
                  color: active || done ? scheme.primary : scheme.onSurfaceVariant)),
        ],
      );
    }

    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        node(0),
        Expanded(
          child: Padding(
            padding: const EdgeInsets.only(top: 14),
            child: Container(height: 2.5, color: photoDone ? scheme.primary : scheme.outlineVariant),
          ),
        ),
        node(1),
        Expanded(
          child: Padding(
            padding: const EdgeInsets.only(top: 14),
            child: Container(height: 2.5, color: locDone && photoDone ? scheme.primary : scheme.outlineVariant),
          ),
        ),
        node(2),
      ],
    );
  }

  Widget _photoArea(ColorScheme scheme) {
    if (_photo == null) {
      return GestureDetector(
        onTap: _submitting ? null : () => _pick(ImageSource.camera),
        child: Container(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [scheme.primaryContainer.withAlpha(150), scheme.surfaceContainerHighest],
            ),
          ),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Container(
                padding: const EdgeInsets.all(18),
                decoration: BoxDecoration(
                  color: scheme.surface,
                  shape: BoxShape.circle,
                  boxShadow: [
                    BoxShadow(color: Colors.black.withAlpha(25), blurRadius: 14, offset: const Offset(0, 4)),
                  ],
                ),
                child: Icon(Icons.photo_camera_rounded, size: 38, color: scheme.primary),
              ),
              const SizedBox(height: 14),
              Text('Tap to photograph the garbage',
                  style: TextStyle(fontWeight: FontWeight.w700, fontSize: 16, color: scheme.onSurface)),
              const SizedBox(height: 4),
              Text('Get close and keep the whole pile in the frame',
                  style: TextStyle(fontSize: 12, color: scheme.onSurfaceVariant)),
            ],
          ),
        ),
      );
    }
    return Stack(
      fit: StackFit.expand,
      children: [
        Image.file(_photo!, fit: BoxFit.cover),
        Positioned(
          left: 12,
          top: 12,
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
            decoration: BoxDecoration(color: Colors.black.withAlpha(140), borderRadius: BorderRadius.circular(20)),
            child: const Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(Icons.check_circle, color: Colors.white, size: 14),
                SizedBox(width: 6),
                Text('Photo ready', style: TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w600)),
              ],
            ),
          ),
        ),
        Positioned(
          right: 8,
          top: 8,
          child: IconButton.filledTonal(
            onPressed: _submitting ? null : () => setState(() => _photo = null),
            icon: const Icon(Icons.close_rounded),
            tooltip: 'Remove photo',
          ),
        ),
      ],
    );
  }

  Widget _locationCard(ColorScheme scheme) {
    final ok = _position != null;
    final info = _accuracyInfo(scheme);
    final tint = ok ? info.$2 : (_locating ? scheme.outline : scheme.error);

    return SoftCard(
      padding: const EdgeInsets.fromLTRB(14, 12, 6, 12),
      child: Row(
        children: [
          Container(
            width: 46,
            height: 46,
            decoration: BoxDecoration(color: tint.withAlpha(34), borderRadius: BorderRadius.circular(14)),
            child: _locating
                ? const Padding(padding: EdgeInsets.all(13), child: CircularProgressIndicator(strokeWidth: 2.4))
                : Icon(ok ? Icons.my_location : Icons.location_off, color: tint),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  ok
                      ? '${_position!.latitude.toStringAsFixed(5)}, ${_position!.longitude.toStringAsFixed(5)}'
                      : (_locating ? 'Getting your location…' : 'Location unavailable'),
                  style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 15),
                ),
                const SizedBox(height: 2),
                Text(
                  _locationError ?? (ok ? '${info.$1} · ±${_position!.accuracy.round()} m' : 'Needed to tag the report'),
                  style: TextStyle(fontSize: 12, color: _locationError != null ? scheme.error : scheme.onSurfaceVariant),
                ),
              ],
            ),
          ),
          IconButton(
            onPressed: _locating ? null : _locate,
            icon: const Icon(Icons.refresh_rounded),
            tooltip: 'Refresh location',
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final canSubmit = _photo != null && _position != null && !_submitting;
    final hint = _photo == null
        ? 'Add a photo to continue'
        : (_position == null ? 'Waiting for your location…' : 'Ready to submit');

    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 18, 16, 24),
      children: [
        Text('Hello, ${Session.firstName} 👋',
            style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w800)),
        const SizedBox(height: 2),
        Text('Spotted garbage? Report it in three quick steps.',
            style: TextStyle(color: scheme.onSurfaceVariant)),
        const SizedBox(height: 16),
        SoftCard(padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14), child: _stepper(scheme)),
        const SizedBox(height: 12),

        // ----- photo -----
        SoftCard(
          padding: EdgeInsets.zero,
          child: Column(
            children: [
              AspectRatio(aspectRatio: 4 / 3, child: _photoArea(scheme)),
              Padding(
                padding: const EdgeInsets.all(14),
                child: Row(
                  children: [
                    Expanded(
                      child: FilledButton.icon(
                        onPressed: _submitting ? null : () => _pick(ImageSource.camera),
                        icon: const Icon(Icons.photo_camera_rounded),
                        label: Text(_photo == null ? 'Camera' : 'Retake'),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: OutlinedButton.icon(
                        onPressed: _submitting ? null : () => _pick(ImageSource.gallery),
                        icon: const Icon(Icons.photo_library_outlined),
                        label: const Text('Gallery'),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),

        // ----- location -----
        _locationCard(scheme),
        const SizedBox(height: 18),

        // ----- submit -----
        FilledButton(
          onPressed: canSubmit ? _submit : null,
          style: FilledButton.styleFrom(
            minimumSize: const Size(64, 56),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          ),
          child: _submitting
              ? Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2.4, color: scheme.onPrimary)),
                    const SizedBox(width: 12),
                    const Text('Analysing photo…'),
                  ],
                )
              : const Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(Icons.send_rounded, size: 20),
                    SizedBox(width: 10),
                    Text('Submit report'),
                  ],
                ),
        ),
        const SizedBox(height: 8),
        Center(
          child: Text(hint,
              style: TextStyle(
                  fontSize: 13, fontWeight: FontWeight.w600, color: canSubmit ? scheme.primary : scheme.onSurfaceVariant)),
        ),
        const SizedBox(height: 16),

        // ----- tips -----
        SoftCard(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const SectionTitle(icon: Icons.tips_and_updates_outlined, text: 'Tips for a good report'),
              const SizedBox(height: 10),
              _tip(scheme, Icons.crop_free, 'Keep the whole pile in the frame.'),
              _tip(scheme, Icons.wb_sunny_outlined, 'Use daylight; avoid blurry or dark photos.'),
              _tip(scheme, Icons.place_outlined, 'Stand close to the spot so the GPS tag is accurate.'),
              _tip(scheme, Icons.auto_awesome, 'AI rates the severity. Worse piles are cleaned sooner.'),
            ],
          ),
        ),
      ],
    );
  }

  Widget _tip(ColorScheme scheme, IconData icon, String text) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 17, color: scheme.primary),
          const SizedBox(width: 10),
          Expanded(child: Text(text, style: TextStyle(fontSize: 13, height: 1.35, color: scheme.onSurfaceVariant))),
        ],
      ),
    );
  }
}

/// Bottom sheet shown after a successful submission.
class _ResultSheet extends StatelessWidget {
  final Complaint complaint;
  final VoidCallback onDone;
  final VoidCallback onViewReports;
  const _ResultSheet({required this.complaint, required this.onDone, required this.onViewReports});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final c = complaint;

    Widget next(IconData icon, String text) => Padding(
          padding: const EdgeInsets.symmetric(vertical: 5),
          child: Row(children: [
            Icon(icon, size: 18, color: scheme.primary),
            const SizedBox(width: 10),
            Expanded(child: Text(text, style: TextStyle(color: scheme.onSurfaceVariant, fontSize: 13.5))),
          ]),
        );

    return SafeArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(24, 0, 24, 20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.check_circle_rounded, color: scheme.primary, size: 46),
            const SizedBox(height: 8),
            Text('Report submitted',
                style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w800)),
            Text('Report #${c.id}', style: TextStyle(color: scheme.onSurfaceVariant)),
            const SizedBox(height: 20),
            SeverityRing(severity: c.severity, size: 112),
            const SizedBox(height: 12),
            Text('${severityWord(c.severity)} severity',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800, color: severityColor(c.severity))),
            const SizedBox(height: 14),
            Wrap(
              alignment: WrapAlignment.center,
              spacing: 8,
              runSpacing: 8,
              children: [
                InfoPill(icon: Icons.delete_outline, label: c.garbageType),
                InfoPill(icon: Icons.crop_free, label: '${c.coveragePct.round()}% of the photo', color: const Color(0xFF2471A3)),
                InfoPill(icon: Icons.timer_outlined, label: 'Target: ${c.slaLabel}', color: const Color(0xFFB9770E)),
                if (c.needsReview)
                  const InfoPill(icon: Icons.flag_outlined, label: 'Manual review', color: Color(0xFF7D3C98)),
              ],
            ),
            const SizedBox(height: 18),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: scheme.primaryContainer.withAlpha(100),
                borderRadius: BorderRadius.circular(16),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('What happens next', style: TextStyle(fontWeight: FontWeight.w800, color: scheme.onSurface)),
                  const SizedBox(height: 6),
                  next(Icons.engineering_outlined, 'A field worker is assigned.'),
                  next(Icons.verified_outlined, 'They clean it and confirm on site with GPS.'),
                  next(Icons.toll_outlined, 'You earn $kCoinsPerCleanup Clean Coins when it is verified.'),
                ],
              ),
            ),
            const SizedBox(height: 18),
            Row(
              children: [
                Expanded(child: OutlinedButton(onPressed: onViewReports, child: const Text('Track it'))),
                const SizedBox(width: 10),
                Expanded(child: FilledButton(onPressed: onDone, child: const Text('Done'))),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
