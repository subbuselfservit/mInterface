#import <Cordova/CDV.h>

#import <Foundation/Foundation.h>
@import UIKit;
#import <MapKit/MapKit.h>
#import <CoreLocation/CoreLocation.h>
#import "Utils.h"
#import <Cordova/CDVPlugin.h>

@interface HWPHello : CDVPlugin <CLLocationManagerDelegate>
- (void)StartService:(CDVInvokedUrlCommand*)command;
- (void)GetCurrentLocation:(CDVInvokedUrlCommand*)command;
- (void)SendLocation:(CDVInvokedUrlCommand*)command;
- (void)getLastKnownLocation:(CDVInvokedUrlCommand*)command;
- (void)pluginResultForTimer:(CDVInvokedUrlCommand*)command;
//- (void)timeReader:(CDVInvokedUrlCommand*)command;
//- (void)CheckSumIndicatorResult:(CDVInvokedUrlCommand*)command;

@property (strong, nonatomic) CLLocationManager * locationManager;
@property (strong, nonatomic) NSMutableArray * coordinatesArray;
@end
