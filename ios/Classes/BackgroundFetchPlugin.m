#import "BackgroundFetchPlugin.h"
#import <TSBackgroundFetch/TSBackgroundFetch.h>

static NSString *const PLUGIN_PATH = @"com.transistorsoft/flutter_background_fetch";
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
    TSBackgroundFetch *fetchManager = [TSBackgroundFetch sharedInstance];
    [fetchManager performFetchWithCompletionHandler:completionHandler applicationState:application.applicationState];
    return YES;
}

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    
    [[TSBackgroundFetch sharedInstance] registerBackgroundFetchTask:BACKGROUND_FETCH_TASK_ID];
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
        [self stop:result];
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

    [fetchManager configure:params callback:^(UIBackgroundRefreshStatus status) {
        if (status != UIBackgroundRefreshStatusAvailable) {
            NSLog(@"- %@ failed to start, status: %lu", PLUGIN_PATH, (long)status);
            result([FlutterError errorWithCode: [NSString stringWithFormat:@"%lu", (long) status] message:nil details:@(status)]);
            return;
        }
        [fetchManager addListener:BACKGROUND_FETCH_TASK_ID callback:[self createCallback]];
        [fetchManager start:BACKGROUND_FETCH_TASK_ID];
        result(@(status));
    }];
}

-(void) start:(FlutterResult)result {
    TSBackgroundFetch *fetchManager = [TSBackgroundFetch sharedInstance];
    [fetchManager start:BACKGROUND_FETCH_TASK_ID callback:^(UIBackgroundRefreshStatus status) {
        if (status == UIBackgroundRefreshStatusAvailable) {
            result(@(status));
        } else {
            NSLog(@"- %@ failed to start, status: %lu", PLUGIN_PATH, (long)status);
            result([FlutterError errorWithCode: [NSString stringWithFormat:@"%lu", (long) status] message:nil details:@(status)]);
        }
    }];
}

-(void) stop:(FlutterResult)result {
    TSBackgroundFetch *fetchManager = [TSBackgroundFetch sharedInstance];
    [fetchManager stop:BACKGROUND_FETCH_TASK_ID];
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
    
    NSError *error = [[TSBackgroundFetch sharedInstance] scheduleTask:taskId delay:delay callback:[self createCallback]];
    
    if (!error) {
        result(@(YES));
    } else {
        result([FlutterError errorWithCode: [NSString stringWithFormat:@"%lu", (long) error.code] message:nil details:error.domain]);
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
