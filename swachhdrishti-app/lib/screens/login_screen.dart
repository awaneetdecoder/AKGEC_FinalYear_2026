import 'package:flutter/material.dart';

import '../api.dart';

class LoginScreen extends StatefulWidget {
  final VoidCallback onLoggedIn;
  const LoginScreen({super.key, required this.onLoggedIn});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _email = TextEditingController();
  final _name = TextEditingController();
  final _server = TextEditingController(text: Session.baseUrl);
  bool _busy = false;
  bool _showServer = false;
  String? _error;

  @override
  void dispose() {
    _email.dispose();
    _name.dispose();
    _server.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    Session.baseUrl = _server.text.trim().replaceAll(RegExp(r'/+$'), '');
    try {
      await Api.login(email: _email.text.trim(), name: _name.text.trim());
      widget.onLoggedIn();
    } on ApiException catch (e) {
      setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Form(
                key: _formKey,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Icon(Icons.eco_rounded, size: 64, color: scheme.primary),
                    const SizedBox(height: 8),
                    Text('SwachhDrishti',
                        textAlign: TextAlign.center,
                        style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w700)),
                    Text('See it. Snap it. Get it cleaned.',
                        textAlign: TextAlign.center, style: TextStyle(color: scheme.onSurfaceVariant)),
                    const SizedBox(height: 28),
                    TextFormField(
                      controller: _email,
                      keyboardType: TextInputType.emailAddress,
                      autofillHints: const [AutofillHints.email],
                      decoration: const InputDecoration(labelText: 'E-mail', prefixIcon: Icon(Icons.mail_outline), border: OutlineInputBorder()),
                      validator: (v) => (v == null || !v.contains('@') || !v.contains('.')) ? 'Enter a valid e-mail' : null,
                    ),
                    const SizedBox(height: 14),
                    TextFormField(
                      controller: _name,
                      textCapitalization: TextCapitalization.words,
                      decoration: const InputDecoration(labelText: 'Your name', prefixIcon: Icon(Icons.person_outline), border: OutlineInputBorder()),
                      validator: (v) => (v == null || v.trim().isEmpty) ? 'Enter your name' : null,
                    ),
                    if (_showServer) ...[
                      const SizedBox(height: 14),
                      TextFormField(
                        controller: _server,
                        keyboardType: TextInputType.url,
                        decoration: const InputDecoration(
                          labelText: 'Server address',
                          helperText: 'Emulator: http://10.0.2.2:8080 · Real phone: http://<your-PC-IP>:8080',
                          prefixIcon: Icon(Icons.dns_outlined),
                          border: OutlineInputBorder(),
                        ),
                        validator: (v) => (v == null || !v.startsWith('http')) ? 'Must start with http:// or https://' : null,
                      ),
                    ],
                    if (_error != null) ...[
                      const SizedBox(height: 14),
                      Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(color: scheme.errorContainer, borderRadius: BorderRadius.circular(10)),
                        child: Text(_error!, style: TextStyle(color: scheme.onErrorContainer)),
                      ),
                    ],
                    const SizedBox(height: 20),
                    FilledButton(
                      onPressed: _busy ? null : _submit,
                      style: FilledButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 14)),
                      child: _busy
                          ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2))
                          : const Text('Continue'),
                    ),
                    TextButton(
                      onPressed: () => setState(() => _showServer = !_showServer),
                      child: Text(_showServer ? 'Hide server settings' : 'Server settings'),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
