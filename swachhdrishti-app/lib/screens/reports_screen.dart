import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../models.dart';
import '../widgets.dart';

enum _Filter { all, open, cleaned }

/// The citizen's own reports: summary, filters, photo, severity and live status.
class ReportsScreen extends StatefulWidget {
  final List<Complaint> complaints;
  final bool loading;
  final String? error;
  final Future<void> Function() onRefresh;

  const ReportsScreen({
    super.key,
    required this.complaints,
    required this.loading,
    required this.error,
    required this.onRefresh,
  });

  @override
  State<ReportsScreen> createState() => _ReportsScreenState();
}

class _ReportsScreenState extends State<ReportsScreen> {
  _Filter _filter = _Filter.all;

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(onRefresh: widget.onRefresh, child: _body(context));
  }

  Widget _message(
    BuildContext context, {
    required IconData icon,
    required Color color,
    required String title,
    required String subtitle,
    Widget? action,
  }) {
    final scheme = Theme.of(context).colorScheme;
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.symmetric(horizontal: 32),
      children: [
        const SizedBox(height: 90),
        Center(
          child: Container(
            padding: const EdgeInsets.all(26),
            decoration: BoxDecoration(color: color.withAlpha(28), shape: BoxShape.circle),
            child: Icon(icon, size: 52, color: color),
          ),
        ),
        const SizedBox(height: 20),
        Text(title,
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800)),
        const SizedBox(height: 8),
        Text(subtitle, textAlign: TextAlign.center, style: TextStyle(color: scheme.onSurfaceVariant, height: 1.45)),
        if (action != null) ...[
          const SizedBox(height: 20),
          Center(child: action),
        ],
      ],
    );
  }

  Widget _body(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final all = widget.complaints;

    if (widget.loading && all.isEmpty) {
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        children: const [SizedBox(height: 200), Center(child: CircularProgressIndicator())],
      );
    }
    if (widget.error != null && all.isEmpty) {
      return _message(
        context,
        icon: Icons.cloud_off_rounded,
        color: scheme.error,
        title: 'Something went wrong',
        subtitle: widget.error!,
        action: FilledButton.icon(
          onPressed: widget.onRefresh,
          icon: const Icon(Icons.refresh_rounded),
          label: const Text('Try again'),
        ),
      );
    }
    if (all.isEmpty) {
      return _message(
        context,
        icon: Icons.eco_rounded,
        color: scheme.primary,
        title: 'No reports yet',
        subtitle: 'Photograph garbage from the Report tab and it will show up here with live status.',
      );
    }

    final openCount = all.where((c) => c.isOpen).length;
    final doneCount = all.where((c) => c.isDone).length;
    final List<Complaint> shown;
    switch (_filter) {
      case _Filter.open:
        shown = all.where((c) => c.isOpen).toList();
      case _Filter.cleaned:
        shown = all.where((c) => c.isDone).toList();
      case _Filter.all:
        shown = all;
    }

    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(16, 18, 16, 24),
      children: [
        Row(children: [
          Expanded(child: _Stat(value: '${all.length}', label: 'Total', color: scheme.primary)),
          const SizedBox(width: 10),
          Expanded(child: _Stat(value: '$openCount', label: 'In progress', color: const Color(0xFFB9770E))),
          const SizedBox(width: 10),
          Expanded(child: _Stat(value: '$doneCount', label: 'Cleaned', color: const Color(0xFF1E8449))),
        ]),
        const SizedBox(height: 14),
        Wrap(spacing: 8, children: [
          _chip(context, 'All', _Filter.all),
          _chip(context, 'In progress', _Filter.open),
          _chip(context, 'Cleaned', _Filter.cleaned),
        ]),
        const SizedBox(height: 12),
        if (shown.isEmpty)
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 40),
            child: Text('Nothing in this list yet.',
                textAlign: TextAlign.center, style: TextStyle(color: scheme.onSurfaceVariant)),
          )
        else
          for (final c in shown) _ReportCard(complaint: c),
      ],
    );
  }

  Widget _chip(BuildContext context, String label, _Filter f) {
    final scheme = Theme.of(context).colorScheme;
    final selected = _filter == f;
    return ChoiceChip(
      label: Text(label),
      selected: selected,
      onSelected: (_) => setState(() => _filter = f),
      selectedColor: scheme.primary,
      showCheckmark: false,
      labelStyle: TextStyle(
        fontWeight: FontWeight.w700,
        color: selected ? scheme.onPrimary : scheme.onSurface,
      ),
    );
  }
}

class _Stat extends StatelessWidget {
  final String value;
  final String label;
  final Color color;
  const _Stat({required this.value, required this.label, required this.color});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return SoftCard(
      padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 10),
      child: Column(children: [
        Text(value, style: TextStyle(fontSize: 24, fontWeight: FontWeight.w800, color: color)),
        const SizedBox(height: 2),
        Text(label, style: TextStyle(color: scheme.onSurfaceVariant, fontSize: 12.5)),
      ]),
    );
  }
}

Widget _thumb(BuildContext context, String url, {double size = 58}) {
  final scheme = Theme.of(context).colorScheme;
  Widget placeholder() => Container(
        width: size,
        height: size,
        color: scheme.surfaceContainerHighest,
        child: Icon(Icons.image_outlined, color: scheme.onSurfaceVariant),
      );
  if (url.isEmpty) return ClipRRect(borderRadius: BorderRadius.circular(14), child: placeholder());
  return ClipRRect(
    borderRadius: BorderRadius.circular(14),
    child: Image.network(
      url,
      width: size,
      height: size,
      fit: BoxFit.cover,
      errorBuilder: (_, __, ___) => placeholder(),
    ),
  );
}

class _ReportCard extends StatelessWidget {
  final Complaint complaint;
  const _ReportCard({required this.complaint});

  @override
  Widget build(BuildContext context) {
    final c = complaint;
    final scheme = Theme.of(context).colorScheme;
    final overdue = c.isOpen && c.slaBreached;

    return SoftCard(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(14),
      onTap: () => showModalBottomSheet<void>(
        context: context,
        isScrollControlled: true,
        showDragHandle: true,
        shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(28))),
        builder: (_) => _Detail(complaint: c),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              _thumb(context, c.photoUrl),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(children: [
                      Expanded(
                        child: Text(c.garbageType,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
                      ),
                      SeverityRing(severity: c.severity, size: 34),
                    ]),
                    const SizedBox(height: 2),
                    Text('#${c.id} · ${timeAgo(c.createdAt)}',
                        style: TextStyle(color: scheme.onSurfaceVariant, fontSize: 12.5)),
                    const SizedBox(height: 6),
                    Wrap(spacing: 6, runSpacing: 4, children: [
                      StatusChip(status: c.status),
                      if (overdue) InfoPill(icon: Icons.warning_amber_rounded, label: 'Overdue', color: scheme.error),
                    ]),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          StatusTimeline(complaint: c),
        ],
      ),
    );
  }
}

class _Detail extends StatelessWidget {
  final Complaint complaint;
  const _Detail({required this.complaint});

  void _viewPhoto(BuildContext context) {
    showDialog<void>(
      context: context,
      builder: (dialogContext) => Dialog.fullscreen(
        backgroundColor: Colors.black,
        child: Stack(
          children: [
            Center(
              child: InteractiveViewer(
                child: Image.network(
                  complaint.photoUrl,
                  fit: BoxFit.contain,
                  errorBuilder: (_, __, ___) =>
                      const Text('Photo not available', style: TextStyle(color: Colors.white70)),
                ),
              ),
            ),
            Positioned(
              top: 40,
              right: 12,
              child: IconButton.filledTonal(
                onPressed: () => Navigator.of(dialogContext).pop(),
                icon: const Icon(Icons.close_rounded),
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final c = complaint;
    final scheme = Theme.of(context).colorScheme;

    Widget row(IconData icon, String k, String v) => Padding(
          padding: const EdgeInsets.symmetric(vertical: 7),
          child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Icon(icon, size: 18, color: scheme.primary),
            const SizedBox(width: 10),
            SizedBox(width: 108, child: Text(k, style: TextStyle(color: scheme.onSurfaceVariant))),
            Expanded(child: Text(v, style: const TextStyle(fontWeight: FontWeight.w600))),
          ]),
        );

    Widget note(IconData icon, Color color, String text) => Container(
          margin: const EdgeInsets.only(top: 12),
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(color: color.withAlpha(28), borderRadius: BorderRadius.circular(14)),
          child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Icon(icon, size: 18, color: color),
            const SizedBox(width: 10),
            Expanded(child: Text(text, style: const TextStyle(height: 1.4))),
          ]),
        );

    return SafeArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(children: [
              Expanded(
                child: Text('Report #${c.id}',
                    style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800)),
              ),
              StatusChip(status: c.status),
            ]),
            const SizedBox(height: 12),
            if (c.photoUrl.isNotEmpty)
              GestureDetector(
                onTap: () => _viewPhoto(context),
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(16),
                  child: Stack(
                    children: [
                      Image.network(
                        c.photoUrl,
                        height: 190,
                        width: double.infinity,
                        fit: BoxFit.cover,
                        errorBuilder: (_, __, ___) => Container(
                          height: 80,
                          width: double.infinity,
                          color: scheme.surfaceContainerHighest,
                          alignment: Alignment.center,
                          child: const Text('Photo not available'),
                        ),
                      ),
                      Positioned(
                        right: 10,
                        bottom: 10,
                        child: Container(
                          padding: const EdgeInsets.all(6),
                          decoration: BoxDecoration(color: Colors.black.withAlpha(130), shape: BoxShape.circle),
                          child: const Icon(Icons.zoom_out_map, size: 18, color: Colors.white),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            const SizedBox(height: 14),
            StatusTimeline(complaint: c),
            const SizedBox(height: 8),
            row(Icons.speed, 'Severity', '${c.severity} of 5 · ${severityWord(c.severity)}'),
            row(Icons.delete_outline, 'Garbage type', c.garbageType),
            row(Icons.crop_free, 'Coverage', '${c.coveragePct.round()}% of the photo'),
            row(Icons.timer_outlined, 'Cleanup target', c.slaLabel),
            row(Icons.place_outlined, 'Location', '${c.latitude.toStringAsFixed(5)}, ${c.longitude.toStringAsFixed(5)}'),
            if (c.status == 'REOPENED')
              note(Icons.replay_circle_filled_outlined, scheme.error,
                  'This spot was reported again after a cleanup, so the ticket was reopened and prioritised.'),
            if (c.isDone)
              note(Icons.verified_outlined, scheme.primary,
                  'Verified cleaned on site. 🪙 Clean Coins were added to your balance.'),
            const SizedBox(height: 14),
            OutlinedButton.icon(
              onPressed: () {
                Clipboard.setData(ClipboardData(text: '${c.latitude}, ${c.longitude}'));
                showAppSnack(context, 'Location copied');
              },
              icon: const Icon(Icons.copy_rounded, size: 18),
              label: const Text('Copy location'),
            ),
          ],
        ),
      ),
    );
  }
}
