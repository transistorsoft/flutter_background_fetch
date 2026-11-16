// (WO-011) A static guard: stop() is declared as the types declare it, stop(taskId?): Promise<boolean>.  This file does
// not compile against a stop() that resolves anything but a bool.  It is a file of its own so that a compile error
// here cannot keep background_fetch_test.dart from running.
import 'package:background_fetch/background_fetch.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('WO-011 stop()', () {
    test('stop is a Future<bool> Function([String?])', () {
      final Future<bool> Function([String?]) stop = BackgroundFetch.stop;
      expect(stop, isA<Function>());
    });
  });
}
