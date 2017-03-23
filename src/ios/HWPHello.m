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

- (void)StartService:(CDVInvokedUrlCommand*)command
{
    //self.locationManager = [CLLocationManager new];
    /*self.locationManager = [[CLLocationManager alloc] init];
    [self.locationManager setDelegate:self];
    [self.locationManager setDistanceFilter:kCLDistanceFilterNone];
    [self.locationManager setHeadingFilter:kCLHeadingFilterNone];
    [self.locationManager requestAlwaysAuthorization];
    [self.locationManager requestWhenInUseAuthorization];
    // Allow background Update
    if ([[[UIDevice currentDevice] systemVersion] floatValue] >= 9) {
        _locationManager.allowsBackgroundLocationUpdates = YES;
    }
    [self.locationManager startUpdatingLocation];
    // [_locationManager startMonitoringSignificantLocationChanges];
    
    [NSTimer scheduledTimerWithTimeInterval:30.0
                                     target:self
                                   selector:@selector(SendLocation:)
                                   userInfo:nil
                                    repeats:YES];*/
    [NSTimer scheduledTimerWithTimeInterval:60.0
                                     target:self
                                   selector:@selector(timeReader:)
                                   userInfo:nil
                                    repeats:YES];
    [NSTimer scheduledTimerWithTimeInterval:1.0
                                     target:self
                                   selector:@selector(DespatchQueue:)
                                   userInfo:nil
                                    repeats:YES];
    [NSTimer scheduledTimerWithTimeInterval:180.0
                                     target:self
                                   selector:@selector(CheckSumIndicatorResult:)
                                   userInfo:nil
                                    repeats:YES];
     CDVPluginResult *result = nil;
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}
- (void)SendLocation22:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        CDVPluginResult *result = nil;
        @try {
            double lat = self.locationManager.location.coordinate.latitude;
            double lngt = self.locationManager.location.coordinate.longitude;
            NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
            [dateFormatter setDateFormat:@"yyyyMMddHHmmss"];
            NSString *content = [NSString stringWithFormat:@"%f,%f,%@\n", lat, lngt, [dateFormatter stringFromDate:[NSDate date]]];
            //To check internet is available or not
            InternetConnection *networkReachability = [InternetConnection reachabilityForInternetConnection];
            NetworkStatus networkStatus = [networkReachability currentReachabilityStatus];
            if(networkStatus == NotReachable){
            } else {
                //send Location updates to server if network is available
                NSString *docdir = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
                NSString *user_file_path = [NSString stringWithFormat:@"%@%@",docdir,@"/mservice/user.txt"];
                NSString *locationData = content;
                
                NSData *user_data = [NSData dataWithContentsOfFile:user_file_path];
                NSError *jsonError = nil;
                NSMutableDictionary * dict = [NSJSONSerialization JSONObjectWithData:user_data options:NSJSONReadingMutableContainers error:&jsonError];
                NSString *clientID = dict[@"client_id"];
                NSString *countryCode = dict[@"country_code"];
                NSString *deviceID = dict[@"device_id"];
                //For xml parsing
                NSString *access_pack_path = [NSString stringWithFormat:@"%@%@%@/%@/%@",docdir,@"/mservice/client_functional_access_package/",clientID,countryCode,@"client_functional_access.xml"];
                //Convert XML to JSON
                [XMLConverter convertXMLFile:access_pack_path completion:^(BOOL success, NSDictionary *dictionary, NSError *error)
                 {
                     if (success) {
                         NSDictionary * dict = [dictionary objectForKey:@"functional_access_detail"];
                         NSString *domain_name = [dict objectForKey:@"domain_name"];
                         NSString *port_no = [dict objectForKey:@"port_no"];
                         NSString *protocol_type = [dict objectForKey:@"protocol_type"];
                         //Send Data to server
                         NSString *baseURL = [NSString stringWithFormat:@"%@//%@:%@/common/components/GeoLocation/update_device_location_offline.aspx",protocol_type,domain_name, port_no];
                         NSString *content = [NSString stringWithFormat:@"<location_xml><client_id>%@</client_id><country_code>%@</country_code><device_id>%@</device_id><location>%@</location></location_xml>", clientID, countryCode, deviceID, locationData];
                         NSData *data = [content dataUsingEncoding:NSUTF8StringEncoding];
                         NSMutableURLRequest *request = [[NSMutableURLRequest alloc] initWithURL:[NSURL URLWithString:baseURL]];
                         [request setValue:@"text/xml" forHTTPHeaderField:@"Content-type"];
                         [request setHTTPMethod : @"POST"];
                         [request setHTTPBody : data];
                         /*if([[NSFileManager defaultManager] fileExistsAtPath:getfullPath isDirectory:false]){
                             // Dealloc txt file
                             [[NSData data] writeToFile:getfullPath atomically:true];
                         }*/
                         // generates an autoreleased NSURLConnection
                         [NSURLConnection connectionWithRequest:request delegate:self];
                     }
                 }];
                result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:content];
                [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
            }
        } @catch (NSException *exception) {
            NSString *fail_reason = [NSString stringWithFormat:@"%@\n%@\n%@",exception.name, exception.reason, exception.description ];
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:fail_reason];
            NSLog(@"Exception name : %@",exception.name);
            NSLog(@"Exception Reason: %@",exception.reason);
            NSLog(@"Exception is : %@", exception.description);
        }
    }];
}

@end
