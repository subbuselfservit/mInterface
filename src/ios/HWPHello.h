#import <Cordova/CDV.h>

#import <Foundation/Foundation.h>
@import UIKit;
#import <MapKit/MapKit.h>
#import <CoreLocation/CoreLocation.h>
#import "Utils.h"
#import <Cordova/CDVPlugin.h>

@interface HWPHello : CDVPlugin <CLLocationManagerDelegate>
- (void)StartService:(CDVInvokedUrlCommand*)command;
- (void)SendLocation;
- (void)getLastKnownLocation:(CDVInvokedUrlCommand*)command;
- (void)GetCurrentLocation;
- (void)pluginResultForTimer:(CDVInvokedUrlCommand*)command;
- (void)timeReader:(CDVInvokedUrlCommand*)command;
- (void)CheckSumIndicatorResult:(CDVInvokedUrlCommand*)command;

@property (strong, nonatomic) CLLocationManager * locationManager;
@property (strong, nonatomic) NSMutableArray * coordinatesArray;
@end
