import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../api.dart';
import '../theme.dart';

/// Sign in / create account. The same e-mail and password work for both.
class AuthScreen extends StatefulWidget {
  final VoidCallback onAuthenticated;
  const AuthScreen({super.key, required this.onAuthenticated});

  @override
  State<AuthScreen> createState() => _AuthScreenState();
}

class _AuthScreenState extends State<AuthScreen> {
  final _formKey = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _email = TextEditingController();
  final _pass = TextEditingController();
  final _pass2 = TextEditingController();
  final _server = TextEditingController(text: Session.baseUrl);

  bool _register = false;
  bool _busy = false;
  bool _hidePass = true;
  bool _showServer = false;
  bool _testing = false;
  String? _error;
  String? _serverNote;
  bool _serverOk = false;

  @override
  void initState() {
    super.initState();
    _autoDetect();
  }

  /// Looks for the server by itself (saved address, USB address, emulator address).
  Future<void> _autoDetect() async {
    setState(() {
      _testing = true;
      _serverNote = null;
    });
    final found = await Api.findServer();
    if (!mounted) return;
    setState(() {
      _testing = false;
      if (found != null) {
        _server.text = found;
        _serverOk = true;
        _serverNote = 'Connected to $found';
      } else {
        _serverOk = false;
        _serverNote = 'Server not found. Tap here to fix it.';
        _showServer = true;
      }
    });
    if (found != null) await Session.setBaseUrl(found);
  }

  @override
  void dispose() {
    _name.dispose();
    _email.dispose();
    _pass.dispose();
    _pass2.dispose();
    _server.dispose();
    super.dispose();
  }

  void _switchTo(bool register) {
    setState(() {
      _register = register;
      _error = null;
    });
  }

  Future<void> _testServer() async {
    setState(() {
      _testing = true;
      _serverNote = null;
    });
    final ok = await Api.ping(_server.text);
    if (!mounted) return;
    setState(() {
      _testing = false;
      _serverOk = ok;
      _serverNote = ok ? 'Server reachable' : 'Cannot reach this address. Check the IP, Wi-Fi and firewall.';
    });
  }

  Future<void> _submit() async {
    FocusScope.of(context).unfocus();
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    await Session.setBaseUrl(_server.text);
    try {
      if (_register) {
        await Api.register(name: _name.text.trim(), email: _email.text.trim(), password: _pass.text);
      } else {
        await Api.login(email: _email.text.trim(), password: _pass.text);
      }
      HapticFeedback.lightImpact();
      widget.onAuthenticated();
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  InputDecoration _decoration(
    BuildContext context, {
    required String label,
    required IconData icon,
    String? helper,
    Widget? suffix,
  }) {
    return InputDecoration(
      labelText: label,
      helperText: helper,
      helperMaxLines: 3,
      prefixIcon: Icon(icon),
      suffixIcon: suffix,
    );
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final text = Theme.of(context).textTheme;

    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.light,
      child: Scaffold(
        body: Container(
          width: double.infinity,
          height: double.infinity,
          decoration: const BoxDecoration(gradient: kBrandGradient),
          child: SafeArea(
            child: Center(
              child: SingleChildScrollView(
                keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
                padding: const EdgeInsets.all(22),
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 430),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      // ----- brand header -----
                      Center(
                        child: Container(
                          width: 84,
                          height: 84,
                          decoration: BoxDecoration(
                            color: Colors.white,
                            borderRadius: BorderRadius.circular(26),
                            boxShadow: [
                              BoxShadow(color: Colors.black.withAlpha(50), blurRadius: 24, offset: const Offset(0, 10)),
                            ],
                          ),
                          child: const Icon(Icons.eco_rounded, size: 46, color: kBrandSeed),
                        ),
                      ),
                      const SizedBox(height: 16),
                      Text('SwachhDrishti',
                          textAlign: TextAlign.center,
                          style: text.headlineMedium?.copyWith(
                              fontWeight: FontWeight.w800, color: Colors.white, letterSpacing: 0.4)),
                      const SizedBox(height: 4),
                      Text('See it. Snap it. Get it cleaned.',
                          textAlign: TextAlign.center,
                          style: TextStyle(color: Colors.white.withAlpha(225), fontSize: 14)),
                      const SizedBox(height: 24),

                      // ----- form card -----
                      Container(
                        padding: const EdgeInsets.fromLTRB(22, 20, 22, 12),
                        decoration: BoxDecoration(
                          color: scheme.surface,
                          borderRadius: BorderRadius.circular(28),
                          boxShadow: [
                            BoxShadow(color: Colors.black.withAlpha(50), blurRadius: 30, offset: const Offset(0, 14)),
                          ],
                        ),
                        child: Form(
                          key: _formKey,
                          child: Column(
                            mainAxisSize: MainAxisSize.min,
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            children: [
                              _tabs(scheme),
                              const SizedBox(height: 20),
                              Text(_register ? 'Create your account' : 'Welcome back 👋',
                                  style: text.titleLarge?.copyWith(fontWeight: FontWeight.w800)),
                              const SizedBox(height: 4),
                              Text(
                                _register
                                    ? 'Join in a minute and start reporting garbage near you.'
                                    : 'Sign in to report garbage and earn Clean Coins.',
                                style: TextStyle(color: scheme.onSurfaceVariant, fontSize: 13),
                              ),
                              const SizedBox(height: 12),
                              _serverPill(scheme),
                              const SizedBox(height: 14),
                              AnimatedSize(
                                duration: const Duration(milliseconds: 220),
                                curve: Curves.easeInOut,
                                alignment: Alignment.topCenter,
                                child: _register
                                    ? Padding(
                                        padding: const EdgeInsets.only(bottom: 14),
                                        child: TextFormField(
                                          controller: _name,
                                          textCapitalization: TextCapitalization.words,
                                          textInputAction: TextInputAction.next,
                                          decoration: _decoration(context, label: 'Full name', icon: Icons.person_outline),
                                          validator: (v) => (v == null || v.trim().isEmpty) ? 'Enter your full name' : null,
                                        ),
                                      )
                                    : const SizedBox(width: double.infinity),
                              ),
                              TextFormField(
                                controller: _email,
                                keyboardType: TextInputType.emailAddress,
                                textInputAction: TextInputAction.next,
                                autofillHints: const [AutofillHints.email],
                                decoration: _decoration(context, label: 'E-mail', icon: Icons.mail_outline),
                                validator: (v) =>
                                    RegExp(r'^\S+@\S+\.\S+$').hasMatch((v ?? '').trim()) ? null : 'Enter a valid e-mail address',
                              ),
                              const SizedBox(height: 14),
                              TextFormField(
                                controller: _pass,
                                obscureText: _hidePass,
                                textInputAction: _register ? TextInputAction.next : TextInputAction.done,
                                onFieldSubmitted: (_) {
                                  if (!_register) _submit();
                                },
                                decoration: _decoration(
                                  context,
                                  label: 'Password',
                                  icon: Icons.lock_outline,
                                  helper: _register ? 'At least 8 characters' : null,
                                  suffix: IconButton(
                                    icon: Icon(_hidePass ? Icons.visibility_outlined : Icons.visibility_off_outlined),
                                    onPressed: () => setState(() => _hidePass = !_hidePass),
                                  ),
                                ),
                                validator: (v) {
                                  final t = v ?? '';
                                  if (t.isEmpty) return 'Enter your password';
                                  if (_register && t.length < 8) return 'Use at least 8 characters';
                                  return null;
                                },
                              ),
                              AnimatedSize(
                                duration: const Duration(milliseconds: 220),
                                curve: Curves.easeInOut,
                                alignment: Alignment.topCenter,
                                child: _register
                                    ? Padding(
                                        padding: const EdgeInsets.only(top: 14),
                                        child: TextFormField(
                                          controller: _pass2,
                                          obscureText: _hidePass,
                                          textInputAction: TextInputAction.done,
                                          onFieldSubmitted: (_) => _submit(),
                                          decoration: _decoration(context,
                                              label: 'Confirm password', icon: Icons.lock_reset_outlined),
                                          validator: (v) => v == _pass.text ? null : 'The passwords do not match',
                                        ),
                                      )
                                    : const SizedBox(width: double.infinity),
                              ),
                              AnimatedSize(
                                duration: const Duration(milliseconds: 220),
                                curve: Curves.easeInOut,
                                alignment: Alignment.topCenter,
                                child: _showServer ? _serverBox(scheme) : const SizedBox(width: double.infinity),
                              ),
                              if (_error != null) ...[
                                const SizedBox(height: 14),
                                Container(
                                  padding: const EdgeInsets.all(12),
                                  decoration: BoxDecoration(
                                    color: scheme.errorContainer,
                                    borderRadius: BorderRadius.circular(12),
                                  ),
                                  child: Row(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Icon(Icons.error_outline, size: 20, color: scheme.onErrorContainer),
                                      const SizedBox(width: 10),
                                      Expanded(child: Text(_error!, style: TextStyle(color: scheme.onErrorContainer))),
                                    ],
                                  ),
                                ),
                              ],
                              const SizedBox(height: 20),
                              FilledButton(
                                onPressed: _busy ? null : _submit,
                                child: _busy
                                    ? SizedBox(
                                        height: 22,
                                        width: 22,
                                        child: CircularProgressIndicator(strokeWidth: 2.4, color: scheme.onPrimary),
                                      )
                                    : Row(
                                        mainAxisAlignment: MainAxisAlignment.center,
                                        children: [
                                          Text(_register ? 'Create account' : 'Sign in'),
                                          const SizedBox(width: 8),
                                          const Icon(Icons.arrow_forward_rounded, size: 20),
                                        ],
                                      ),
                              ),
                              const SizedBox(height: 4),
                              TextButton(
                                onPressed: _busy ? null : () => _switchTo(!_register),
                                child: Text(_register ? 'Already have an account? Sign in' : 'New here? Create an account'),
                              ),
                              TextButton.icon(
                                onPressed: () => setState(() => _showServer = !_showServer),
                                icon: Icon(_showServer ? Icons.expand_less : Icons.settings_ethernet, size: 18),
                                label: Text(_showServer ? 'Hide server settings' : 'Server settings'),
                                style: TextButton.styleFrom(foregroundColor: scheme.onSurfaceVariant),
                              ),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(height: 16),
                      Text(
                        'Field workers and officers: please use the web portal.',
                        textAlign: TextAlign.center,
                        style: TextStyle(color: Colors.white.withAlpha(215), fontSize: 12.5),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  /// Small connection indicator: green when the server was found.
  Widget _serverPill(ColorScheme scheme) {
    final Color c;
    final String label;
    if (_testing) {
      c = scheme.outline;
      label = 'Looking for the server…';
    } else if (_serverOk) {
      c = const Color(0xFF2E9E5B);
      label = _serverNote ?? 'Connected';
    } else {
      c = scheme.error;
      label = _serverNote ?? 'Server not checked';
    }
    return GestureDetector(
      onTap: _testing ? null : _autoDetect,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        decoration: BoxDecoration(color: c.withAlpha(28), borderRadius: BorderRadius.circular(12)),
        child: Row(
          children: [
            _testing
                ? const SizedBox(height: 14, width: 14, child: CircularProgressIndicator(strokeWidth: 2))
                : Icon(_serverOk ? Icons.check_circle : Icons.error_outline, size: 16, color: c),
            const SizedBox(width: 8),
            Expanded(
              child: Text(label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(fontSize: 12.5, fontWeight: FontWeight.w600, color: c)),
            ),
            if (!_testing) Icon(Icons.refresh_rounded, size: 16, color: c),
          ],
        ),
      ),
    );
  }

  Widget _serverBox(ColorScheme scheme) {
    return Padding(
      padding: const EdgeInsets.only(top: 14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          TextFormField(
            controller: _server,
            keyboardType: TextInputType.url,
            decoration: _decoration(
              context,
              label: 'Server address',
              icon: Icons.dns_outlined,
              helper: 'USB: http://127.0.0.1:8080 · Emulator: http://10.0.2.2:8080 · Wi-Fi: http://<PC-IP>:8080',
            ),
            validator: (v) => (v == null || !v.trim().startsWith('http')) ? 'Must start with http:// or https://' : null,
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              OutlinedButton.icon(
                onPressed: _testing ? null : _testServer,
                icon: _testing
                    ? const SizedBox(height: 16, width: 16, child: CircularProgressIndicator(strokeWidth: 2))
                    : const Icon(Icons.network_check, size: 18),
                label: const Text('Test connection'),
                style: OutlinedButton.styleFrom(minimumSize: const Size(0, 42)),
              ),
              const SizedBox(width: 10),
              if (_serverNote != null)
                Expanded(
                  child: Text(
                    _serverNote!,
                    style: TextStyle(
                      fontSize: 12.5,
                      fontWeight: FontWeight.w600,
                      color: _serverOk ? scheme.primary : scheme.error,
                    ),
                  ),
                ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _tabs(ColorScheme scheme) {
    Widget tab(String label, bool selected, VoidCallback onTap) {
      return Expanded(
        child: GestureDetector(
          onTap: _busy ? null : onTap,
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 180),
            padding: const EdgeInsets.symmetric(vertical: 12),
            decoration: BoxDecoration(
              color: selected ? scheme.primary : Colors.transparent,
              borderRadius: BorderRadius.circular(12),
            ),
            child: Text(
              label,
              textAlign: TextAlign.center,
              style: TextStyle(
                fontWeight: FontWeight.w700,
                color: selected ? scheme.onPrimary : scheme.onSurfaceVariant,
              ),
            ),
          ),
        ),
      );
    }

    return Container(
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        color: scheme.surfaceContainerHighest.withAlpha(110),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(children: [
        tab('Sign in', !_register, () => _switchTo(false)),
        tab('Create account', _register, () => _switchTo(true)),
      ]),
    );
  }
}
