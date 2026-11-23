import 'package:flutter/material.dart';

import 'models.dart';

/// Rounded, softly outlined card used across the app. Optionally tappable.
class SoftCard extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry padding;
  final EdgeInsetsGeometry margin;
  final VoidCallback? onTap;

  const SoftCard({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.all(16),
    this.margin = EdgeInsets.zero,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final radius = BorderRadius.circular(22);
    return Padding(
      padding: margin,
      child: Container(
        decoration: BoxDecoration(
          borderRadius: radius,
          border: Border.all(color: scheme.outlineVariant.withAlpha(140)),
          boxShadow: [
            BoxShadow(color: Colors.black.withAlpha(14), blurRadius: 14, offset: const Offset(0, 5)),
          ],
        ),
        child: Material(
          color: scheme.surface,
          borderRadius: radius,
          clipBehavior: Clip.antiAlias,
          child: InkWell(
            onTap: onTap,
            child: Padding(padding: padding, child: child),
          ),
        ),
      ),
    );
  }
}

/// Circular gauge showing severity out of 5.
class SeverityRing extends StatelessWidget {
  final int severity;
  final double size;
  const SeverityRing({super.key, required this.severity, this.size = 48});

  @override
  Widget build(BuildContext context) {
    final color = severityColor(severity);
    return SizedBox(
      width: size,
      height: size,
      child: Stack(
        alignment: Alignment.center,
        children: [
          SizedBox.expand(
            child: CircularProgressIndicator(
              value: severity / 5,
              strokeWidth: size * 0.11,
              backgroundColor: color.withAlpha(45),
              valueColor: AlwaysStoppedAnimation<Color>(color),
            ),
          ),
          Text('$severity', style: TextStyle(fontSize: size * 0.4, fontWeight: FontWeight.w800, color: color)),
        ],
      ),
    );
  }
}

/// Coloured pill showing a complaint status.
class StatusChip extends StatelessWidget {
  final String status;
  const StatusChip({super.key, required this.status});

  @override
  Widget build(BuildContext context) {
    final color = statusColor(status);
    final done = status == 'CLEANED' || status == 'CLOSED';
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 4),
      decoration: BoxDecoration(color: color.withAlpha(34), borderRadius: BorderRadius.circular(20)),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(done ? Icons.check_circle : Icons.circle, size: done ? 13 : 8, color: color),
          const SizedBox(width: 5),
          Flexible(
            child: Text(
              statusLabel(status),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(fontSize: 11.5, fontWeight: FontWeight.w700, color: color),
            ),
          ),
        ],
      ),
    );
  }
}

/// Small icon + text pill.
class InfoPill extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color? color;
  const InfoPill({super.key, required this.icon, required this.label, this.color});

  @override
  Widget build(BuildContext context) {
    final c = color ?? Theme.of(context).colorScheme.primary;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(color: c.withAlpha(30), borderRadius: BorderRadius.circular(12)),
      child: Row(mainAxisSize: MainAxisSize.min, children: [
        Icon(icon, size: 15, color: c),
        const SizedBox(width: 5),
        Flexible(
          child: Text(label,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(color: c, fontSize: 12.5, fontWeight: FontWeight.w700)),
        ),
      ]),
    );
  }
}

/// Section heading with an icon.
class SectionTitle extends StatelessWidget {
  final IconData icon;
  final String text;
  const SectionTitle({super.key, required this.icon, required this.text});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Row(children: [
      Icon(icon, color: scheme.primary, size: 22),
      const SizedBox(width: 8),
      Text(text, style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800)),
    ]);
  }
}

/// Received -> Assigned -> Cleaned progress line (or a "reopened" notice).
class StatusTimeline extends StatelessWidget {
  final Complaint complaint;
  const StatusTimeline({super.key, required this.complaint});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final c = complaint;

    if (c.status == 'REOPENED') {
      return Container(
        padding: const EdgeInsets.all(10),
        decoration: BoxDecoration(color: scheme.error.withAlpha(28), borderRadius: BorderRadius.circular(12)),
        child: Row(children: [
          Icon(Icons.replay_circle_filled_outlined, color: scheme.error, size: 20),
          const SizedBox(width: 8),
          Expanded(
            child: Text('Reopened: the spot was reported again, so it has been prioritised.',
                style: TextStyle(color: scheme.error, fontSize: 12.5, fontWeight: FontWeight.w600)),
          ),
        ]),
      );
    }

    final step = c.isDone ? 2 : (c.status == 'ASSIGNED' ? 1 : 0);
    const labels = ['Received', 'Assigned', 'Cleaned'];

    Widget dot(int i) {
      final reached = i <= step;
      return Container(
        width: 20,
        height: 20,
        decoration: BoxDecoration(
          color: reached ? scheme.primary : scheme.surface,
          shape: BoxShape.circle,
          border: Border.all(color: reached ? scheme.primary : scheme.outlineVariant, width: 2),
        ),
        child: reached ? Icon(Icons.check, size: 12, color: scheme.onPrimary) : null,
      );
    }

    return Column(
      children: [
        Row(children: [
          for (var i = 0; i < 3; i++) ...[
            dot(i),
            if (i < 2)
              Expanded(child: Container(height: 3, color: i < step ? scheme.primary : scheme.outlineVariant)),
          ],
        ]),
        const SizedBox(height: 6),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            for (var i = 0; i < 3; i++)
              Text(labels[i],
                  style: TextStyle(
                    fontSize: 11.5,
                    fontWeight: i <= step ? FontWeight.w700 : FontWeight.w500,
                    color: i <= step ? scheme.primary : scheme.onSurfaceVariant,
                  )),
          ],
        ),
      ],
    );
  }
}

/// "just now", "5 min ago", "3 h ago", "2 d ago", "12 Oct".
String timeAgo(DateTime? d) {
  if (d == null) return '';
  final diff = DateTime.now().difference(d);
  if (diff.inMinutes < 1) return 'just now';
  if (diff.inMinutes < 60) return '${diff.inMinutes} min ago';
  if (diff.inHours < 24) return '${diff.inHours} h ago';
  if (diff.inDays < 7) return '${diff.inDays} d ago';
  const m = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  return '${d.day} ${m[d.month - 1]}';
}

/// Floating message at the bottom of the screen.
void showAppSnack(BuildContext context, String message, {bool error = false}) {
  final scheme = Theme.of(context).colorScheme;
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(
      content: Text(message),
      backgroundColor: error ? scheme.error : null,
    ));
}
