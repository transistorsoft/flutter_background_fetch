#import "BackgroundFetchPlugin.h"
#import <TSBackgroundFetch/TSBackgroundFetch.h>

static NSString *const PLUGIN_PATH = @"com.transistorsoft/flutter_background_fetch";
static NSString *const METHOD_CHANNEL_NAME      = @"methods";
static NSString *const EVENT_CHANNEL_NAME       = @"events";

static NSString *const ACTION_CONFIGURE = @"configure";
static NSString *const ACTION_START     = @"start";
static NSString *const ACTION_STOP      = @"stop";
static NSString *const ACTION_FINISH    = @"finish";
static NSString *const ACTION_STATUS    = @"status";
static NSString *const ACTION_REGISTER_HEADLESS_TASK = @"registerHeadlessTask";


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

+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
    NSString *methodPath = [NSString stringWithFormat:@"%@/%@", PLUGIN_PATH, METHOD_CHANNEL_NAME];
    FlutterMethodChannel* channel = [FlutterMethodChannel methodChannelWithName:methodPath binaryMessenger:[registrar messenger]];
    
    BackgroundFetchPlugin* instance = [[BackgroundFetchPlugin alloc] init];
    [registrar addApplicationDelegate:instance];
    [registrar addMethodCallDelegate:instance channel:channel];
    
    NSString *eventPath = [NSString stringWithFormat:@"%@/%@", PLUGIN_PATH, EVENT_CHANNEL_NAME];
    
    FlutterEventChannel* eventChannel = [FlutterEventChannel eventChannelWithName:eventPath binaryMessenger:[registrar messenger]];
    [eventChannel setStreamHandler:instance];
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
        [self finish:[call.arguments integerValue] result:result];
    } else if ([self method:call.method is:ACTION_REGISTER_HEADLESS_TASK]) {
        result(@(YES));
    } else {
        result(FlutterMethodNotImplemented);
    }
}

-(void) configure:(NSDictionary*)params result:(FlutterResult)result {
    TSBackgroundFetch *fetchManager = [TSBackgroundFetch sharedInstance];
    
    [fetchManager configure:params callback:^(UIBackgroundRefreshStatus status) {
        if (status != UIBackgroundRefreshStatusAvailable) {
            NSLog(@"- %@ failed to start, status: %lu", PLUGIN_PATH, status);
            result([FlutterError errorWithCode: [NSString stringWithFormat:@"%lu", (long) status] message:nil details:@(status)]);
            return;
        }
        void (^handler)(void);
        handler = ^void(void){
            if (self->eventSink != nil) {
                self->eventSink(@(YES));
            }
        };
        [fetchManager addListener:PLUGIN_PATH callback:handler];
        [fetchManager start];
        result(@(status));
    }];
}

-(void) start:(FlutterResult)result {
    TSBackgroundFetch *fetchManager = [TSBackgroundFetch sharedInstance];
    [fetchManager start:^(UIBackgroundRefreshStatus status) {
        if (status == UIBackgroundRefreshStatusAvailable) {
            result(@(status));
        } else {
            NSLog(@"- %@ failed to start, status: %lu", PLUGIN_PATH, status);
            result([FlutterError errorWithCode: [NSString stringWithFormat:@"%lu", (long) status] message:nil details:@(status)]);
        }
    }];
}

-(void) stop:(FlutterResult)result {
    TSBackgroundFetch *fetchManager = [TSBackgroundFetch sharedInstance];
    [fetchManager stop];
    [self status:result];
}

-(void) status:(FlutterResult)result {
    [[TSBackgroundFetch sharedInstance] status:^(UIBackgroundRefreshStatus status) {
        result(@(status));
    }];
}

-(void) finish:(NSInteger)fetchResult result:(FlutterResult)flutterResult {
    UIBackgroundFetchResult result = UIBackgroundFetchResultNewData;
    if (fetchResult == UIBackgroundFetchResultNewData
        || fetchResult == UIBackgroundFetchResultNoData
        || fetchResult == UIBackgroundFetchResultFailed) {
        result = fetchResult;
    }
    TSBackgroundFetch *fetchManager = [TSBackgroundFetch sharedInstance];
    [fetchManager finish:PLUGIN_PATH result:result];
    flutterResult(@(YES));
}

- (BOOL) method:(NSString*)method is:(NSString*)action {
    return [method isEqualToString:action];
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
