#import <Flutter/Flutter.h>

@interface BackgroundFetchPlugin : NSObject<FlutterPlugin>
+ (void)registerBackgroundTasksIfNeeded;
@end
