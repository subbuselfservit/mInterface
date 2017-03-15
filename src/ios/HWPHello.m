#import "HWPHello.h"
#import "XMLConverter.h"
#import "InternetConnection.h"

@implementation HWPHello

#pragma mark - MapViewDelegate
- (void) locationManager:(CLLocationManager *)manager didUpdateLocations:(NSArray<CLLocation *> *)locations{
    //NSLog(@"%@",[locations lastObject]);
   CLLocation* location = [locations lastObject];
    NSDate* eventDate = location.timestamp;
    NSTimeInterval howRecent = [eventDate timeIntervalSinceNow];
    if (fabs(howRecent) < 10.0) {
        // If the event is recent, do something with it.
        /*NSLog(@"latitude %+.6f, longitude %+.6f\n",
              location.coordinate.latitude,
              location.coordinate.longitude);*/
    }
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
    self.locationManager = [[CLLocationManager alloc] init];
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
    
    CDVPluginResult *result = nil;
   // NSTimer *getLocationTimer = 
        [NSTimer scheduledTimerWithTimeInterval:30.0
                                         target:self
                                       selector:@selector(GetCurrentLocation:)
                                       userInfo:nil
                                        repeats:YES];
    //NSTimer *sendLocationTimer = 
        [NSTimer scheduledTimerWithTimeInterval:30.0
                                         target:self
                                       selector:@selector(SendLocation:)
                                       userInfo:nil
                                        repeats:YES];
    //[[NSRunLoop currentRunLoop] addTimer:getLocationTimer forMode:NSRunLoopCommonModes];
    //[[NSRunLoop currentRunLoop] addTimer:sendLocationTimer forMode:NSRunLoopCommonModes];
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    //[result setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (void)GetCurrentLocation:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        //code for directory and file creation
        NSArray *docDir = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
        NSError *error;
        NSString *fullPath = [[docDir objectAtIndex:0] stringByAppendingPathComponent:@"/mservice"];
        NSData *fileContents = [[NSData alloc] init];
        // Check if folder is exists
        if (![[NSFileManager defaultManager] fileExistsAtPath:fullPath]){
            // Create folder if not exists
            [[NSFileManager defaultManager] createDirectoryAtPath:fullPath withIntermediateDirectories:YES attributes:nil error:&error];
        }
        fullPath = [fullPath stringByAppendingString:@"/MyLocation.txt"];
        NSFileManager *filemanager = [NSFileManager defaultManager];
        //check if file is not exists
        if([filemanager fileExistsAtPath:fullPath] == YES){
            NSLog(@"File Exsists");
        } else {
            //create if file is not exsits
            [fileContents writeToFile:fullPath atomically:true];
        }
        double lat = self.locationManager.location.coordinate.latitude;
        double lngt = self.locationManager.location.coordinate.longitude;
        NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
        [dateFormatter setDateFormat:@"yyyyMMddHHmmss"];
        
        NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
        NSString *documentsDirectory = [paths objectAtIndex:0];
        NSString *fileName = [NSString stringWithFormat:@"%@/mservice/MyLocation.txt", documentsDirectory];
        NSString *content = [NSString stringWithFormat:@"%f,%f,%@\n", lat, lngt, [dateFormatter stringFromDate:[NSDate date]]];
        //Code for file writing with appending
        NSFileHandle *fileHandle = [NSFileHandle fileHandleForWritingAtPath:fileName];
        [fileHandle seekToEndOfFile];
        [fileHandle writeData:[content dataUsingEncoding:NSUTF8StringEncoding]];
        [fileHandle closeFile];
    }];
}

- (void)SendLocation:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        //To check internet is available or not
        InternetConnection *networkReachability = [InternetConnection reachabilityForInternetConnection];
        NetworkStatus networkStatus = [networkReachability currentReachabilityStatus];
        if(networkStatus == NotReachable){
            NSLog(@"There is no internet connection");
        } else {
            NSLog(@"There is internet conncetion available");
            //send Location updates to server if network is available
            NSString *docdir = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
            NSString *folderPath = @"/mservice/MyLocation.txt";
        
            NSString *user_file_path = [NSString stringWithFormat:@"%@%@",docdir,@"/mservice/user.txt"];
            NSString *docFullPath = [NSString stringWithFormat:@"%@%@",docdir,folderPath];
            NSError *error;
            NSString *locationData = [NSString stringWithContentsOfFile:docFullPath encoding:NSUTF8StringEncoding error:&error];
        
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
                 if([[NSFileManager defaultManager] fileExistsAtPath:docFullPath isDirectory:false]){
                     // Dealloc txt file
                     [[NSData data] writeToFile:docFullPath atomically:true];
                 }
                 // generates an autoreleased NSURLConnection
                 [NSURLConnection connectionWithRequest:request delegate:self];
             }
             }];
        }
    }];
}

- (void)getLastKnownLocation:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
     CDVPluginResult *result = nil;
     double lat = self.locationManager.location.coordinate.latitude;
     double lngt = self.locationManager.location.coordinate.longitude;
     NSString *locationString = [NSString stringWithFormat:@"{\"lat\":\"%f\",\"lon\":\"%f\"}", lat, lngt];
     result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:locationString];
     [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }];
}

- (void)pluginResultForTimer:(CDVInvokedUrlCommand*)command
{   
   [self.commandDelegate runInBackground:^{
    double lat = self.locationManager.location.coordinate.latitude;
    double lngt = self.locationManager.location.coordinate.longitude;
    NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
    [dateFormatter setDateFormat:@"yyyyMMddHHmmss"];
 
    NSString *docdir = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
    NSString *user_file_path = [NSString stringWithFormat:@"%@%@",docdir,@"/mservice/user.txt"];
    NSData *user_data = [NSData dataWithContentsOfFile:user_file_path];
    NSError *jsonError = nil;
    NSMutableDictionary *dict = [NSJSONSerialization JSONObjectWithData:user_data options:NSJSONReadingMutableContainers error:&jsonError];
    NSString *clientID = dict[@"client_id"];
    NSString *countryCode = dict[@"country_code"];
    NSString *deviceID = dict[@"device_id"];
    NSString *locationData = [NSString stringWithFormat:@"%f,%f,%@\n", lat, lngt, [dateFormatter stringFromDate:[NSDate date]]];
    
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
             // generates an autoreleased NSURLConnection
             [NSURLConnection connectionWithRequest:request delegate:self];
         }
     }];
    }];
}
@end
