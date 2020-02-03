#include "AppDelegate.h"
#include "GeneratedPluginRegistrant.h"
#include <TSBackgroundFetch/TSBackgroundFetch.h>

@implementation AppDelegate

- (BOOL)application:(UIApplication *)application
    didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
  [GeneratedPluginRegistrant registerWithRegistry:self];
  // Override point for customization after application launch.
  TSBackgroundFetch *fetch = [TSBackgroundFetch sharedInstance];
  [fetch registerBackgroundProcessingTask:@"foo"];
    
  return [super application:application didFinishLaunchingWithOptions:launchOptions];
}

@end
