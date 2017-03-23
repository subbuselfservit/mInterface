#import <Cordova/CDV.h>

#import <Foundation/Foundation.h>
@import UIKit;
#import <MapKit/MapKit.h>
#import <CoreLocation/CoreLocation.h>
#import <Cordova/CDVPlugin.h>

@interface HWPHello : CDVPlugin <CLLocationManagerDelegate>
/*- (void)StartService:(CDVInvokedUrlCommand*)command;
- (void)SendLocation:(CDVInvokedUrlCommand*)command;
- (void)GetLocation:(CDVInvokedUrlCommand*)command;
- (void)timeReader:(CDVInvokedUrlCommand*)command;
- (void)CheckSumIndicatorResult:(CDVInvokedUrlCommand*)command;
- (void)CheckLocation:(CDVInvokedUrlCommand*)command;
- (void)DespatchQueue:(CDVInvokedUrlCommand*)command;
+(BOOL)CopyFile:(NSString *)source toDestination:(NSString *)destination;*/
- (void)SendLocation22:(CDVInvokedUrlCommand*)command;
@property (strong, nonatomic) CLLocationManager * locationManager;
@property (strong, nonatomic) NSMutableArray * coordinatesArray;
@end
