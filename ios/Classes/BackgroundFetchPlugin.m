#import "BackgroundFetchPlugin.h"
#import <TSBackgroundFetch/TSBackgroundFetch.h>

static NSString *const PLUGIN_PATH = @"com.transistorsoft/flutter_background_fetch";
static NSString *const PLUGIN_ID = @"flutter_background_fetch";

static NSString *const BACKGROUND_FETCH_TASK_ID = @"com.transistorsoft.fetch";

static NSString *const METHOD_CHANNEL_NAME      = @"methods";
static NSString *const EVENT_CHANNEL_NAME       = @"events";
static NSString *const EVENT_CHANNEL_SCHEDULED_TASKS = @"events/scheduled";

static NSString *const ACTION_CONFIGURE = @"configure";
static NSString *const ACTION_START     = @"start";
static NSString *const ACTION_STOP      = @"stop";
static NSString *const ACTION_FINISH    = @"finish";
static NSString *const ACTION_STATUS    = @"status";
static NSString *const ACTION_REGISTER_HEADLESS_TASK = @"registerHeadlessTask";
static NSString *const ACTION_SCHEDULE_TASK = @"scheduleTask";

@interface BackgroundFetchPlugin ()<FlutterStreamHandler>
@end

@implementation BackgroundFetchPlugin {
    FlutterEventSink eventSink;
}

-(BOOL)application:(UIApplication *)application performFetchWithCompletionHandler:(void (^)(UIBackgroundFetchResult))completionHandler
{
    NSLog(@"BackgroundFetch AppDelegate received fetch event");
    [[TSBackgroundFetch sharedInstance] performFetchWithCompletionHandler:completionHandler applicationState:application.applicationState];
    return YES;
}

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    [[TSBackgroundFetch sharedInstance] didFinishLaunching];
    return YES;
}

+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
    NSString *methodPath = [NSString stringWithFormat:@"%@/%@", PLUGIN_PATH, METHOD_CHANNEL_NAME];
    FlutterMethodChannel* channel = [FlutterMethodChannel methodChannelWithName:methodPath binaryMessenger:[registrar messenger]];

    BackgroundFetchPlugin* instance = [[BackgroundFetchPlugin alloc] init];
    [registrar addApplicationDelegate:instance];
    [registrar addMethodCallDelegate:instance channel:channel];

    NSString *eventPath = [NSString stringWithFormat:@"%@/%@", PLUGIN_PATH, EVENT_CHANNEL_NAME];
    FlutterEventChannel* eventChannel = [FlutterEventChannel eventChannelWithName:eventPath binaryMessenger:[registrar messenger]];
    [eventChannel setStreamHandler:instance];
    
    NSString *scheduledEventPath = [NSString stringWithFormat:@"%@/%@", PLUGIN_PATH, EVENT_CHANNEL_SCHEDULED_TASKS];
    FlutterEventChannel* eventChannelScheduled = [FlutterEventChannel eventChannelWithName:scheduledEventPath binaryMessenger:[registrar messenger]];
    [eventChannelScheduled setStreamHandler:instance];
}

-(instancetype) init {
    self = [super init];
    return self;
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
    if ([self method:call.method is:ACTION_CONFIGURE]) {
        [self configure:call.arguments result:result];
    } else if ([self method:call.method is:ACTION_START]) {
        [self start:result];
    } else if ([self method:call.method is:ACTION_STOP]) {
        [self stop:call.arguments result:result];
    } else if ([self method:call.method is:ACTION_STATUS]) {
        [self status:result];
    } else if ([self method:call.method is:ACTION_FINISH]) {
        [self finish:call.arguments result:result];
    } else if ([self method:call.method is:ACTION_REGISTER_HEADLESS_TASK]) {
        result(@(YES));
    } else if ([self method:call.method is:ACTION_SCHEDULE_TASK]) {
        [self scheduleTask:call.arguments result:result];
    } else {
        result(FlutterMethodNotImplemented);
    }
}

-(void) configure:(NSDictionary*)params result:(FlutterResult)result {
    TSBackgroundFetch *fetchManager = [TSBackgroundFetch sharedInstance];
                
    [fetchManager addListener:PLUGIN_ID callback:[self createCallback]];
    
    NSTimeInterval delay = [[params objectForKey:@"minimumFetchInterval"] doubleValue] * 60;
    [fetchManager configure:delay callback:^(UIBackgroundRefreshStatus status) {        
        if (status != UIBackgroundRefreshStatusAvailable) {
            NSLog(@"- %@ failed to start, status: %lu", PLUGIN_ID, (long)status);
            result([FlutterError errorWithCode: [NSString stringWithFormat:@"%lu", (long) status] message:nil details:@(status)]);
        } else {
            result(@(status));
        }
    }];
}

-(void) start:(FlutterResult)result {
    TSBackgroundFetch *fetchManager = [TSBackgroundFetch sharedInstance];
                                      
    [fetchManager status:^(UIBackgroundRefreshStatus status) {
        if (status == UIBackgroundRefreshStatusAvailable) {
            [fetchManager addListener:PLUGIN_ID callback:[self createCallback]];
            NSError *error = [fetchManager start:PLUGIN_ID];
            if (!error) {
                result(@(status));
            } else {
                result([FlutterError errorWithCode: [NSString stringWithFormat:@"%lu", (long) status] message:error.localizedFailureReason details:nil]);
            }
        } else {
            NSLog(@"- %@ failed to start, status: %lu", PLUGIN_PATH, (long)status);
            result([FlutterError errorWithCode: [NSString stringWithFormat:@"%lu", (long) status] message:@"disabled" details:@(status)]);
        }
    }];
}

-(void) stop:(NSString*)taskId result:(FlutterResult)result {
    TSBackgroundFetch *fetchManager = [TSBackgroundFetch sharedInstance];
    if (!taskId) {
        // Remove fetch listener.
        [fetchManager removeListener:PLUGIN_ID];
    }
    // Calling stop with nil taskId will cause TSBackgroundFetch to stop all custom-tasks from #scheduleTask
    [fetchManager stop:taskId];
    [self status:result];
}

-(void) status:(FlutterResult)result {
    [[TSBackgroundFetch sharedInstance] status:^(UIBackgroundRefreshStatus status) {
        result(@(status));
    }];
}

-(void) finish:(NSString*)taskId result:(FlutterResult)flutterResult {
    TSBackgroundFetch *fetchManager = [TSBackgroundFetch sharedInstance];
    [fetchManager finish:taskId];
    flutterResult(@(YES));
}

- (void) scheduleTask:(NSDictionary*)config result:(FlutterResult)result {
    NSString *taskId = [config objectForKey:@"taskId"];
    long delayMS = [[config objectForKey:@"delay"] longValue];
    NSTimeInterval delay = delayMS / 1000;
    BOOL periodic = [[config objectForKey:@"periodic"] boolValue];
        
    NSError *error = [[TSBackgroundFetch sharedInstance] scheduleProcessingTaskWithIdentifier:taskId
                                                                                        delay:delay
                                                                                     periodic:periodic
                                                                                     callback:[self createCallback]];
    if (!error) {
        result(@(YES));
    } else {
        result([FlutterError errorWithCode: [NSString stringWithFormat:@"%lu", (long) error.code] message:error.localizedFailureReason details:nil]);
    }
}

- (BOOL) method:(NSString*)method is:(NSString*)action {
    return [method isEqualToString:action];
}

-(void (^)(NSString* taskId)) createCallback {
    return ^void(NSString* taskId){
        if (self->eventSink != nil) {
            self->eventSink(taskId);
        }
    };
}
    
#pragma mark FlutterStreamHandler impl

- (FlutterError*)onListenWithArguments:(id)arguments eventSink:(FlutterEventSink)sink {
    eventSink = sink;
    return nil;
}

- (FlutterError*)onCancelWithArguments:(id)arguments {
    eventSink = nil;
    return nil;
}

@end
