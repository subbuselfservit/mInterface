//
//  Utils.h
//  MService
//
//  Copyright © 2016 sample. All rights reserved.
//

#import <Foundation/Foundation.h>
@import UIKit;
#import <MapKit/MapKit.h>
#import <CoreLocation/CoreLocation.h>

@interface Utils : NSObject <CLLocationManagerDelegate>
    @property (strong, nonatomic) CLLocationManager * locationManager;
    @property (strong, nonatomic) NSMutableArray * coordinatesArray;
    + (Utils*)sharedSingleton;
@end
