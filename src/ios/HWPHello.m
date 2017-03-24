#import "HWPHello.h"
#import "XMLConverter.h"
#import "InternetConnection.h"

@implementation HWPHello

#pragma mark - MapViewDelegate
- (void) locationManager:(CLLocationManager *)manager didUpdateLocations:(NSArray<CLLocation *> *)locations{
    //NSLog(@"%@",[locations lastObject]);
    /* CLLocation* location = [locations lastObject];
     NSDate* eventDate = location.timestamp;
     NSTimeInterval howRecent = [eventDate timeIntervalSinceNow];
     if (fabs(howRecent) < 10.0) {
     
     }*/
}
// Error while updating location
- (void) locationManager:(CLLocationManager *)manager didFailWithError:(NSError *)error{
    NSLog(@"%@",error);
}
- (void)locationManager:(CLLocationManager *)manager didUpdateToLocation:(CLLocation *)newLocation fromLocation:(CLLocation *)oldLocation {
    
}
- (void)locationManager:(CLLocationManager *)manager didUpdateHeading:(CLHeading *)newHeading {
    //handle your heading updates here- I would suggest only handling the nth update, because they
    //come in fast and furious and it takes a lot of processing power to handle all of them
}

- (void)SendLocation22:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
       CDVPluginResult *result = nil;
        @try {
                NSString *content = @"hi i'm plugin...";
                result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:content];
                [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
            }  @catch (NSException *exception) {
            NSString *fail_reason = [NSString stringWithFormat:@"%@\n%@\n%@",exception.name, exception.reason, exception.description ];
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:fail_reason];
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
            NSLog(@"Exception name : %@",exception.name);
            NSLog(@"Exception Reason: %@",exception.reason);
            NSLog(@"Exception is : %@", exception.description);
        }
    }];
}

@end
