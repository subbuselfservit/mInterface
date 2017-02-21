#import <Cordova/CDV.h>

#import <Foundation/Foundation.h>
@import UIKit;
#import <MapKit/MapKit.h>
#import <CoreLocation/CoreLocation.h>
#import "Utils.h"

@interface HWPHello : CDVPlugin
    - (void)StartService:(CDVInvokedUrlCommand*)command;
    - (void)SendLocation;
    - (void)getLastKnownLocation:(CDVInvokedUrlCommand*)command;
    - (void)GetCurrentLocation;
@end
