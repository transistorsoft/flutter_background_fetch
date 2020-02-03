//
//  RNBackgroundFetchManager.h
//  RNBackgroundFetch
//
//  Created by Christopher Scott on 2016-08-02.
//  Copyright © 2016 Christopher Scott. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <BackgroundTasks/BackgroundTasks.h>

@interface TSBackgroundFetch : NSObject

@property (nonatomic) BOOL stopOnTerminate;
@property (readonly) BOOL configured;
@property (readonly) BOOL active;

+ (TSBackgroundFetch *)sharedInstance;
-(void) registerBackgroundFetchTask:(NSString*)identifier;
-(void) registerBackgroundProcessingTask:(NSString*)identifier;

-(void) performFetchWithCompletionHandler:(void (^)(UIBackgroundFetchResult))handler applicationState:(UIApplicationState)state;
-(void) configure:(NSDictionary*)config callback:(void(^)(UIBackgroundRefreshStatus status))callback;
-(void) configure:(NSDictionary*)config;
-(void) addListener:(NSString*)componentName callback:(void (^)(NSString*))callback;
-(void) removeListener:(NSString*)componentName;
-(BOOL) hasListener:(NSString*)componentName;
-(void) start:(NSString*)identifier callback:(void(^)(UIBackgroundRefreshStatus status))callback;
-(void) start:(NSString*)identifier;
-(void) stop:(NSString*)identifier;
-(void) finish:(NSString*)tag;
-(void) status:(void(^)(UIBackgroundRefreshStatus status))callback;
-(NSError*) scheduleTask:(NSString*)taskId delay:(NSTimeInterval)delay callback:(void(^)(NSString* taskId))callback;

@end

