// (WO-011) stop() resolves `true` when the native half answers `true`, as @transistorsoft/background-fetch-types
// declares (stop(taskId?): Promise<boolean>), and sends 'stop' with the taskId, or null to stop every task.  The method
// channel stands in for both natives here; the lab's api-smoke and api-smoke-ios read the natives themselves.
import 'package:background_fetch/background_fetch.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const channel = MethodChannel('com.transistorsoft/flutter_background_fetch/methods');
  final calls = <MethodCall>[];
  final messenger = TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;

  setUp(() {
    calls.clear();
    messenger.setMockMethodCallHandler(channel, (MethodCall call) async {
      calls.add(call);
      return call.method == 'stop' ? true : null;
    });
  });

  tearDown(() => messenger.setMockMethodCallHandler(channel, null));

  group('WO-011 stop()', () {
    test('stop() resolves true and sends stop with no taskId', () async {
      expect(await BackgroundFetch.stop(), isTrue);
      expect(calls.single.method, 'stop');
      expect(calls.single.arguments, isNull);
    });

    test("stop('id') resolves true and sends the taskId", () async {
      expect(await BackgroundFetch.stop('id'), isTrue);
      expect(calls.single.method, 'stop');
      expect(calls.single.arguments, 'id');
    });
  });
}
