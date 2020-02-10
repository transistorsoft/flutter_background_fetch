# iOS Setup

## Configure Background Capabilities

- Select the root of your project.  Select **Capabilities** tab.  Enable **Background Modes** and enable the following mode:

- [x] Background fetch
- [x] Background processing (Only if you intend to use `BackgroundFetch.scheduleTask`)

![](https://dl.dropboxusercontent.com/s/9vik5kxoklk63ob/ios-setup-background-modes.png?dl=1)


## Configure `Info.plist`
1.  Open your `Info.plist` and the key *"Permitted background task scheduler identifiers"*

![](https://dl.dropboxusercontent.com/s/t5xfgah2gghqtws/ios-setup-permitted-identifiers.png?dl=1)

2.  Add the **required identifier `com.transistorsoft.fetch`**.

![](https://dl.dropboxusercontent.com/s/kwdio2rr256d852/ios-setup-permitted-identifiers-add.png?dl=1)

3.  If you intend to execute your own custom tasks via **`BackgroundFetch.scheduleTask`**, you must add those custom identifiers as well.  For example, if you intend to execute a custom **`taskId: 'com.foo.customtask'`**, you must add the identifier **`com.foo.customtask`** to your *"Permitted background task scheduler identifiers"*, as well.

```dart
BackgroundFetch.scheduleTask(TaskConfig(
  taskId: 'com.foo.customtask',
  delay: 60 * 60 * 1000  //  In one hour (milliseconds) 
));
```

## `AppDelegate.m`

**If** you added custom *Background Processing* identifier(s) in your `Info.plist` and intend to use **`BackgroundFetch.scheduleTask`**, you must register those custom identifier(s) in your **`AppDelegate`** method **`didFinishLaunchingWithOptions`**:

__Note:__ The SDK *automatically* registers its required fetch-task **`com.transistorsoft.fetch`** &mdash; You need only register **your own** custom task idenfifiers here.

```obj-c
- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
  [GeneratedPluginRegistrant registerWithRegistry:self];
  .
  .
  .
  // [BackgroundFetch] Register your custom Background Processing task(s)
  TSBackgroundFetch *fetch = [TSBackgroundFetch sharedInstance];
  [fetch registerBGProcessingTask:@"com.foo.customtask"];
  .
  .
  .  
  return [super application:application didFinishLaunchingWithOptions:launchOptions];
}

```