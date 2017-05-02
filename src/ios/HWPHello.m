#import "HWPHello.h"
#import "XMLConverter.h"
#import "InternetConnection.h"
#import <AssetsLibrary/AssetsLibrary.h>

@implementation HWPHello
@synthesize btnGallery;

#pragma mark - MapViewDelegate
- (void) locationManager:(CLLocationManager *)manager didUpdateLocations:(NSArray<CLLocation *> *)locations{
    
}
// Error while updating location
- (void) locationManager:(CLLocationManager *)manager didFailWithError:(NSError *)error{
    NSLog(@"%@",error);
}
- (void)locationManager:(CLLocationManager *)manager didUpdateToLocation:(CLLocation *)newLocation fromLocation:(CLLocation *)oldLocation {
    
}
- (void)locationManager:(CLLocationManager *)manager didUpdateHeading:(CLHeading *)newHeading {
    
}

- (void)StartService:(CDVInvokedUrlCommand*)command
{
    @try {
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
        
        self.QueueTimer = [NSTimer scheduledTimerWithTimeInterval:1.0
                                                           target:self
                                                         selector:@selector(DespatchQueue)
                                                         userInfo:nil
                                                          repeats:YES];
        self.LocationTimer = [NSTimer scheduledTimerWithTimeInterval:30.0
                                                              target:self
                                                            selector:@selector(SendLocation)
                                                            userInfo:nil
                                                             repeats:YES];
        self.timeReaderTimer = [NSTimer scheduledTimerWithTimeInterval:60.0
                                                                target:self
                                                              selector:@selector(timeReader)
                                                              userInfo:nil
                                                               repeats:YES];
        self.CheckSumTimer = [NSTimer scheduledTimerWithTimeInterval:180.0
                                                              target:self
                                                            selector:@selector(CheckSumIndicatorResult)
                                                            userInfo:nil
                                                             repeats:YES];
    } @catch (NSException *exception) {
        NSLog(@"Exception is : %@", exception.description);
    }
}

- (void)SendLocation
{
    [self.commandDelegate runInBackground:^{
        @try {
            NSArray *getdocDir = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
            NSError *geterror;
            NSString *parentfolder = [[getdocDir objectAtIndex:0] stringByAppendingPathComponent:@"/mservice"];
            NSString *getfullPath = parentfolder;
            NSData *fileContents = [[NSData alloc] init];
            // Check if mservice folder is exists
            if (![[NSFileManager defaultManager] fileExistsAtPath:getfullPath]){
                // Create folder if not exists
                [[NSFileManager defaultManager] createDirectoryAtPath:getfullPath withIntermediateDirectories:YES attributes:nil error:&geterror];
            }
            //create MyLocation.txt file
            getfullPath = [getfullPath stringByAppendingString:@"/MyLocation.txt"];
            NSFileManager *filemanager = [NSFileManager defaultManager];
            //check if file is not exists
            if([filemanager fileExistsAtPath:getfullPath] == YES){
                NSLog(@"File Exsists");
            } else {
                //create if file is not exsits
                [fileContents writeToFile:getfullPath atomically:true];
            }
            //Create LastKnownLocation.txt file
            NSString *lastKnownPath = parentfolder;
            lastKnownPath = [lastKnownPath stringByAppendingString:@"/LastKnownLocation.txt"];
            if([filemanager fileExistsAtPath:lastKnownPath] == YES){
                NSLog(@"File Exsists");
            } else {
                //create if file is not exsits
                [fileContents writeToFile:lastKnownPath atomically:true];
            }
            
            double lat = self.locationManager.location.coordinate.latitude;
            double lngt = self.locationManager.location.coordinate.longitude;
            NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
            [dateFormatter setDateFormat:@"yyyyMMddHHmmss"];
            NSString *content = [NSString stringWithFormat:@"%f,%f,%@\n", lat, lngt, [dateFormatter stringFromDate:[NSDate date]]];
            
            //Appending Locations in MyLocation.txt file
            NSFileHandle *myLocationFileHandle = [NSFileHandle fileHandleForWritingAtPath:getfullPath];
            [myLocationFileHandle seekToEndOfFile];
            [myLocationFileHandle writeData:[content dataUsingEncoding:NSUTF8StringEncoding]];
            [myLocationFileHandle closeFile];
            //Updating Locations in LastKnownLocation.txt file
            NSData *lastKnownLocationData = [content dataUsingEncoding:NSUTF8StringEncoding];
            [lastKnownLocationData writeToFile:lastKnownPath atomically:true];
            //To check internet is available or not
            InternetConnection *networkReachability = [InternetConnection reachabilityForInternetConnection];
            NetworkStatus networkStatus = [networkReachability currentReachabilityStatus];
            if(networkStatus == NotReachable){
            } else {
                //send Location updates to server if network is available
                NSString *docdir = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
                NSString *user_file_path = [NSString stringWithFormat:@"%@%@",docdir,@"/mservice/user.txt"];
                NSError *error;
                NSString *locationData = [NSString stringWithContentsOfFile:getfullPath encoding:NSUTF8StringEncoding error:&error];
                
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
                         if([[NSFileManager defaultManager] fileExistsAtPath:getfullPath isDirectory:false]){
                             // Dealloc txt file
                             [[NSData data] writeToFile:getfullPath atomically:true];
                         }
                         [NSURLConnection connectionWithRequest:request delegate:self];
                     }
                 }];
            }
        } @catch (NSException *exception) {
            NSLog(@"SendLocation Exception is : %@", exception.description);
        }
    }];
}

- (void)GetLocation:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        CDVPluginResult *result = nil;
        @try {
            NSString *docdir = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
            NSString *filePath = [NSString stringWithFormat:@"%@%@",docdir,@"/mservice/LastKnownLocation.txt"];
            NSError *error;
            NSString *locationData = [NSString stringWithContentsOfFile:filePath encoding:NSUTF8StringEncoding error:&error];
            NSArray *mySplit = [locationData componentsSeparatedByString:@","];
            NSString *locationString = [NSString stringWithFormat:@"{\"lat\":\"%@\",\"lon\":\"%@\"}", [mySplit objectAtIndex:0], [mySplit objectAtIndex:1]];
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:locationString];
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        } @catch (NSException *exception) {
            NSString *fail_reason = [NSString stringWithFormat:@"%@\n%@\n%@",exception.name, exception.reason, exception.description ];
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:fail_reason];
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
            NSLog(@"GetLocation Exception is : %@", exception.description);
        }
    }];
}

- (void)timeReader
{
    [self.commandDelegate runInBackground:^{
        @try {
            NSString *docdir = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
            NSString *folderPath = [NSString stringWithFormat:@"%@/mservice/time_profile.txt", docdir];
            NSData *data = [NSData dataWithContentsOfFile:folderPath];
            NSError *jsonError = nil;
            NSMutableDictionary * dict = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingMutableContainers error:&jsonError];
            NSString *serverDate = dict[@"serverDate"];
            NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
            [dateFormatter setDateFormat:@"yyyy,MM,dd,HH,mm,ss"];
            NSDate *date = [dateFormatter dateFromString:serverDate];
            NSDate *addedDate = [date dateByAddingTimeInterval:(1*60)];
            NSString *dateString = [dateFormatter stringFromDate:addedDate];
            dict[@"serverDate"] = dateString;
            NSData *fileContents = [NSJSONSerialization dataWithJSONObject:dict options:NSJSONWritingPrettyPrinted error:nil];
            [fileContents writeToFile:folderPath atomically:true];
        } @catch (NSException *exception) {
            NSLog(@"timeReader Exception is : %@", exception.description);
        }
    }];
}

- (void)CheckSumIndicatorResult
{
    [self.commandDelegate runInBackground:^{
        @try
        {
            //Read checksum_value.txt file
            NSString *docdir = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
            NSString *checkSumPath = [NSString stringWithFormat:@"%@/mservice/database/checksum_value.txt", docdir];
            NSData *data = [NSData dataWithContentsOfFile:checkSumPath];
            NSString *checksum_value;
            NSString *refresh_ind;
            NSFileManager *filemanager = [NSFileManager defaultManager];
            //To check checksum_value.txt file is exist or not
            if([filemanager fileExistsAtPath:checkSumPath] == YES){
                NSError *jsonError = nil;
                NSMutableDictionary *dict = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingMutableContainers error:&jsonError];
                checksum_value = dict[@"checksum_value"];
                refresh_ind = dict[@"refresh_ind"];
            } else {
                checksum_value = @"";
                refresh_ind = @"";
            }
            //get login_profile data and send it to server
            if([refresh_ind isEqual: @""] || [refresh_ind isEqual:@"false"]){
                NSString *user_profile_path = [NSString stringWithFormat:@"%@/mservice/user_profile.txt", docdir];
                NSData *user_data = [NSData dataWithContentsOfFile:user_profile_path];
                NSError *jsonError = nil;
                NSMutableDictionary * dict = [NSJSONSerialization JSONObjectWithData:user_data options:NSJSONReadingMutableContainers error:&jsonError];
                NSString *user_profile_value = @"login_profile";
                NSArray *protocol = [[dict objectForKey:user_profile_value] valueForKey:@"protocol"];
                NSArray *domain_name = [[dict objectForKey:user_profile_value] valueForKey:@"domain_name"];
                NSArray *portno = [[dict objectForKey:user_profile_value] valueForKey:@"portno"];
                NSString *request_path = [NSString stringWithFormat:@"%@//%@:%@/JSONServiceEndpoint.aspx?appName=common_modules&serviceName=retrieve_listof_values_for_searchcondition&path=context/outputparam",protocol, domain_name, portno];
                
                NSArray *guid_val = [[dict objectForKey:user_profile_value] valueForKey:@"guid_val"];
                NSArray *user_id = [[dict objectForKey:user_profile_value] valueForKey:@"user_id"];
                NSArray *client_id = [[dict objectForKey:user_profile_value] valueForKey:@"client_id"];
                NSArray *locale_id = [[dict objectForKey:user_profile_value] valueForKey:@"locale_id"];
                NSArray *country_code = [[dict objectForKey:user_profile_value] valueForKey:@"country_code"];
                NSArray *emp_id = [[dict objectForKey:user_profile_value] valueForKey:@"emp_id"];
                NSString *content = [NSString stringWithFormat:@"{\"context\":{\"sessionId\":\"%@\",\"userId\":\"%@\",\"client_id\":\"%@\",\"locale_id\":\"%@\",\"country_code\":\"%@\",\"inputparam\":{\"p_inputparam_xml\":\"<inputparam><lov_code_type>VALIDATE_CHECKSUM</lov_code_type><search_field_1>%@</search_field_1><search_field_2>%@</search_field_2><search_field_3>MOBILE</search_field_3></inputparam>\"}}}",guid_val, user_id, client_id, locale_id, country_code, checksum_value, emp_id];
                NSData *data = [content dataUsingEncoding:NSUTF8StringEncoding];
                NSMutableURLRequest *request = [[NSMutableURLRequest alloc] initWithURL:[NSURL URLWithString:request_path]];
                [request setValue:@"text/json" forHTTPHeaderField:@"Content-type"];
                [request setHTTPMethod : @"POST"];
                [request setHTTPBody : data];
                NSURLResponse *response;
                NSError *responseError;
                //send it synchronous
                NSData *responseData = [NSURLConnection sendSynchronousRequest:request returningResponse:&response error:&responseError];
                NSString *responseString = [[NSString alloc] initWithData:responseData encoding:NSUTF8StringEncoding];
                //Replacing resopnse from Array of object to json object
                NSString *replacedString = responseString;
                replacedString = [replacedString stringByReplacingOccurrencesOfString:@"[" withString:@""];
                replacedString = [replacedString stringByReplacingOccurrencesOfString:@"]" withString:@""];
                //Write response into checksum.txt file
                NSFileHandle *fileHandle = [NSFileHandle fileHandleForWritingAtPath:checkSumPath];
                [fileHandle writeData:[replacedString dataUsingEncoding:NSUTF8StringEncoding]];
                [fileHandle closeFile];
                NSString *date, *hour, *minute;
                NSData *responseJson = [replacedString dataUsingEncoding:NSUTF8StringEncoding];
                NSMutableDictionary * dictionary = [NSJSONSerialization JSONObjectWithData:responseJson options:NSJSONReadingMutableContainers error:&jsonError];
                date = [NSString stringWithFormat:@"%@", dictionary[@"serverDate"]];
                hour = [NSString stringWithFormat:@"%@", dictionary[@"serverHour"]];
                minute = [NSString stringWithFormat:@"%@", dictionary[@"serverMinute"]];
                [self timeValues:date hour:hour minute:minute];
            }
        } @catch (NSException *exception) {
            NSLog(@"CheckSumIndicatorResult Exception is : %@", exception.description);
        }
    }];
}

- (void)RefreshTimeProfile:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        NSMutableDictionary * dict = [[command arguments] objectAtIndex:0];
        NSString *date = dict[@"serverDate"];
        NSString *hour = dict[@"serverHour"];
        NSString *minute = dict[@"serverMinute"];
        [self timeValues:date hour:hour minute:minute];
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }];
}

-(void)timeValues:(NSString *)date hour:(NSString *)hour minute:(NSString *)minute
{
    NSString *serverDate = [NSString stringWithFormat:@"%@,%@,%@,00", date, hour, minute];
    NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
    [dateFormatter setDateFormat:@"yyyy,MM,dd,HH,mm,ss"];
    NSDate *getDate = [dateFormatter dateFromString:serverDate];
    NSString *deviceDate = [dateFormatter stringFromDate:[NSDate date]];
    NSString *finalServerDate = [dateFormatter stringFromDate:getDate];
    NSMutableDictionary * dictionary = [[NSMutableDictionary alloc] init];
    [dictionary setValue:finalServerDate forKey:@"serverDate"];
    [dictionary setValue:finalServerDate forKey:@"initServerDate"];
    [dictionary setValue:deviceDate forKey:@"initDeviceDate"];
    NSString *docdir = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
    NSString *time_profile = [NSString stringWithFormat:@"%@/mservice/time_profile.txt", docdir];
    NSLog(@"time profile path : %@", time_profile);
    NSData *fileContents = [NSJSONSerialization dataWithJSONObject:dictionary options:0 error:nil];
    [fileContents writeToFile:time_profile atomically:true];
}

- (void)GetNewDate:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        NSDate *final = nil;
        NSString *docdir = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
        NSString *time_profile = [NSString stringWithFormat:@"%@/mservice/time_profile.txt", docdir];
        NSData *user_data = [NSData dataWithContentsOfFile:time_profile];
        NSError *jsonError = nil;
        NSMutableDictionary * dict = [NSJSONSerialization JSONObjectWithData:user_data options:NSJSONReadingMutableContainers error:&jsonError];
        NSString *serverDate = dict[@"serverDate"];
        NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
        [dateFormatter setDateFormat:@"yyyy,MM,dd,HH,mm,ss"];
        NSDate *deviceDate = [NSDate date];
        NSDate *getDate = [dateFormatter dateFromString:serverDate];
        //NSTimeInterval diff = [deviceDate timeIntervalSinceDate:getDate];
        NSTimeInterval timeDiff = [deviceDate timeIntervalSinceReferenceDate] - [getDate timeIntervalSinceReferenceDate];
        if(timeDiff > 0){
            final = [getDate dateByAddingTimeInterval:timeDiff];
        }
        NSString *finalServerDate = [dateFormatter stringFromDate:final];
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:finalServerDate];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }];
}

- (void)CheckLocation:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
    CDVPluginResult *result = nil;
    @try {
        BOOL isEnabled = false;
        if([CLLocationManager locationServicesEnabled] &&
           [CLLocationManager authorizationStatus] != kCLAuthorizationStatusDenied)
        {
            isEnabled = true;
        } else {
            isEnabled = false;
        }
        NSString *serviceResult = [NSString stringWithFormat:@"%s", isEnabled ? "true" : "false"];
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:serviceResult];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    } @catch (NSException *exception) {
        NSString *fail_reason = [NSString stringWithFormat:@"%@\n%@\n%@",exception.name, exception.reason, exception.description ];
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:fail_reason];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        NSLog(@"CheckLocation Exception is : %@", exception.description);
    }
    }];
}

- (void)CopyFile:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        @try {
            NSMutableDictionary * dict = [[command arguments] objectAtIndex:0];
            NSString* directory = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory,
                                                                      NSUserDomainMask, YES)[0];
            NSString *fromPath = dict[@"srcPath"];
            NSString *toPath = [NSString stringWithFormat:@"%@/%@", directory, dict[@"desPath"]];
            // Check if destination folder is exists
            if (![[NSFileManager defaultManager] fileExistsAtPath:toPath]){
                // Create folder if not exists
                [[NSFileManager defaultManager] createDirectoryAtPath:toPath withIntermediateDirectories:YES attributes:nil error:nil];
            }
            NSString *destFilePath = [NSString stringWithFormat:@"%@/%@", toPath, dict[@"desFile"]];
            NSError *error;
            //Check file already exists or not
            if([[NSFileManager defaultManager] fileExistsAtPath:fromPath])
            {
                if(![[NSFileManager defaultManager] fileExistsAtPath:destFilePath]){
                    if([[NSFileManager defaultManager] copyItemAtPath:fromPath toPath:destFilePath error:&error]==YES)
                    {
                        NSLog(@"File copied..");
                    }
                }
                CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
                [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
            } else {
                CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
                [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
            }
        } @catch (NSException *exception) {
            NSLog(@"CopyFile Exception is : %@", exception.description);
        }
    }];
}

- (void)DespatchQueue
{
    [self.commandDelegate runInBackground:^{
       @try {
            NSString *docdir = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
            NSString *queueFilePath = [NSString stringWithFormat:@"%@/mservice/database/queue_mgr.txt", docdir];
            NSLog(@"queue path : %@", queueFilePath);
            NSString *contents =[NSString stringWithContentsOfFile:queueFilePath encoding:NSUTF8StringEncoding error:nil];
            NSArray *mySplit = [contents componentsSeparatedByString:@"\n"];
            NSString *firstLineData = [mySplit objectAtIndex:0];
            //To check internet is available or not
            InternetConnection *networkReachability = [InternetConnection reachabilityForInternetConnection];
            NetworkStatus networkStatus = [networkReachability currentReachabilityStatus];
            if(networkStatus != NotReachable){
                NSMutableDictionary *bckpDataFullContent;
                NSData *data = [firstLineData dataUsingEncoding:NSUTF8StringEncoding];
                NSError *jsonError = nil;
                NSMutableDictionary * dict = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingMutableContainers error:&jsonError];
                NSString *requestUrl = dict[@"url"];
                NSString *sendData = dict[@"input"];
                NSString *fileType = dict[@"type"];
                NSString *sendFileBasePath = dict[@"filepath"];
                NSString *sendFileName = dict[@"filename"];
                NSString *method = dict[@"method"];
                NSString *keyValue = dict[@"key"];
                //NSString *subKeyValue = dict[@"subkey"];
                if([method isEqualToString:@"read"]){
                    NSString *backupFilePath = [NSString stringWithFormat:@"%@/mservice/database/bckp_%@.txt",docdir, keyValue];
                    //send data to server
                    NSData *dataToServer = [sendData dataUsingEncoding:NSUTF8StringEncoding];
                    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] initWithURL:[NSURL URLWithString:requestUrl]];
                    [request setValue:@"text/json" forHTTPHeaderField:@"Content-type"];
                    [request setHTTPMethod : @"POST"];
                    [request setHTTPBody : dataToServer];
                    //Get Response from url
                    NSURLResponse *response;
                    NSError *responseError;
                    NSData *responseData = [NSURLConnection sendSynchronousRequest:request returningResponse:&response error:&responseError];
                    NSString *responseString = [[NSString alloc] initWithData:responseData encoding:NSUTF8StringEncoding];
                    NSFileManager *filemanager = [NSFileManager defaultManager];
                    //check if file is not exists
                    if([filemanager fileExistsAtPath:backupFilePath] == YES){
                        //Read backp + keyValue file and convert it to a JSON object
                        NSData *data = [NSData dataWithContentsOfFile:backupFilePath];
                        bckpDataFullContent = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingMutableContainers error:&jsonError];
                    } else {
                        //make empty JSON
                        bckpDataFullContent = [[NSMutableDictionary alloc] init];
                    }
                    //write response to backup file where subkey matches in queue mngr file.
                    NSData *dddddd = [responseString dataUsingEncoding:NSUTF8StringEncoding];
                    id json = [NSJSONSerialization JSONObjectWithData:dddddd options:0 error:nil];
                    [bckpDataFullContent setValue:json forKey:dict[@"subkey"]];
                    NSData *bbbbb = [NSJSONSerialization dataWithJSONObject:bckpDataFullContent options:0 error:nil];
                    [bbbbb writeToFile:backupFilePath atomically:true];
                } else {
                    if([fileType isEqualToString:@"file"]){
                        @try {
                            NSString *requestFilePath = [NSString stringWithFormat:@"%@/%@/", docdir, sendFileBasePath];
                            NSLog(@"%@", requestFilePath);
                            NSString *urlString =[NSString stringWithFormat:@"%@&filename=%@",requestUrl, sendFileName];
                            NSURL *url=[NSURL URLWithString:[[NSString stringWithFormat:@"%@", urlString] stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]];
                            
                            // create request
                            NSMutableURLRequest *theRequest = [[NSMutableURLRequest alloc] init];
                            [theRequest setCachePolicy:NSURLRequestReloadIgnoringLocalCacheData];
                            [theRequest setHTTPShouldHandleCookies:NO];
                            [theRequest setTimeoutInterval:30];
                            [theRequest setHTTPMethod:@"POST"];
                            
                            NSString *boundary = @"---------------------------14737809831466499882746641449";
                            NSString *contentType = [NSString stringWithFormat:@"multipart/form-data; boundary=%@",boundary];
                            [theRequest addValue:contentType forHTTPHeaderField: @"Content-Type"];
                            
                            // add image data
                            NSString *filePathofImage = [requestFilePath stringByAppendingPathComponent:sendFileName];
                            NSLog(@"%@", filePathofImage);
                            
                            UIImage *yourImage= [UIImage imageNamed:filePathofImage];
                            NSData *imageData = UIImageJPEGRepresentation(yourImage, 1.0);
                            NSMutableData *body = [NSMutableData data];
                            [body appendData:[[NSString stringWithFormat:@"\r\n--%@\r\n", boundary] dataUsingEncoding:NSUTF8StringEncoding]];
                            if (imageData)
                                [body appendData:[[NSString stringWithFormat:@"%@%@%@", @"Content-Disposition: form-data; name=\"uploaded_file\"; filename=\"",sendFileName,@"\"\r\n"] dataUsingEncoding:NSUTF8StringEncoding]];
                            NSString *imgMimeType = [NSString stringWithFormat:@"Content-Type: %@\r\n\r\n", [self contentTypeForImageData:imageData]];
                            [body appendData:[imgMimeType dataUsingEncoding:NSUTF8StringEncoding]];
                            if (imageData)
                                [body appendData:[NSData dataWithData:imageData]];
                            [body appendData:[[NSString stringWithFormat:@"\r\n--%@--\r\n", boundary] dataUsingEncoding:NSUTF8StringEncoding]];
                            
                            // setting the body of the post to the reqeust
                            [theRequest setHTTPBody:body];
                            
                            NSString *postLength = [NSString stringWithFormat:@"%lu", (unsigned long)[body length]];
                            [theRequest setValue:postLength forHTTPHeaderField:@"Content-Length"];
                            
                            // set URL
                            [theRequest setURL:url];
                            NSURLResponse *response;
                            NSError *responseError;
                            NSData *responseData = [NSURLConnection sendSynchronousRequest:theRequest returningResponse:&response error:&responseError];
                            NSString *responseString = [[NSString alloc] initWithData:responseData encoding:NSUTF8StringEncoding];
                            NSLog(@"sendLocation resposne string : %@", responseString);
                        } @catch (NSException *exception) {
                            NSLog(@"Exception : %@", exception.description);
                        }
                    } else {
                        //send data to server
                        NSData *dataToServer = [sendData dataUsingEncoding:NSUTF8StringEncoding];
                        NSMutableURLRequest *request = [[NSMutableURLRequest alloc] initWithURL:[NSURL URLWithString:requestUrl]];
                        [request setValue:@"text/json" forHTTPHeaderField:@"Content-type"];
                        [request setHTTPMethod : @"POST"];
                        [request setHTTPBody : dataToServer];
                        [NSURLConnection connectionWithRequest:request delegate:self];
                    }
                }
                NSMutableArray *tmp = [mySplit mutableCopy];
                [tmp removeObjectAtIndex:0];
                NSString *finalString = [tmp componentsJoinedByString:@"\n"];
                NSData *finalQueuedata = [finalString dataUsingEncoding:NSUTF8StringEncoding];
                [finalQueuedata writeToFile:queueFilePath atomically:true];
            }
        } @catch (NSException *exception) {
            NSLog(@"DespatchQueue Exception is : %@", exception.description);
        }
    }];
}

- (void)FileChooser:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        ipc= [[UIImagePickerController alloc] init];
        ipc.delegate = self;
        ipc.sourceType = UIImagePickerControllerSourceTypeSavedPhotosAlbum;
        UIViewController *top = [UIApplication sharedApplication].keyWindow.rootViewController;
        if(UI_USER_INTERFACE_IDIOM()==UIUserInterfaceIdiomPhone){
            [top presentViewController:ipc animated:YES completion: nil];
            self.callbackIdForImagePicker = command.callbackId;
        }
        else
        {
            popover=[[UIPopoverController alloc]initWithContentViewController:ipc];
            CGRect myFrame = [top.view frame];
            [popover presentPopoverFromRect:myFrame inView:top.view permittedArrowDirections:UIPopoverArrowDirectionAny animated:YES];
        }
    }];
}

- (NSString *)contentTypeForImageData:(NSData *)data
{
    uint8_t c;
    [data getBytes:&c length:1];
    switch (c) {
        case 0xFF:
            return @"image/jpeg";
        case 0x89:
            return @"image/png";
        case 0x47:
            return @"image/gif";
    }
    return nil;
}

#pragma mark - ImagePickerController Delegate

-(void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary *)info
{
    if(UI_USER_INTERFACE_IDIOM()==UIUserInterfaceIdiomPhone) {
        [picker dismissViewControllerAnimated:YES completion:nil];
    } else {
        [popover dismissPopoverAnimated:YES];
    }
    UIImageView *ivPickedImage = [[UIImageView alloc] init];
    ivPickedImage.image = [info objectForKey:UIImagePickerControllerOriginalImage];
    NSURL *refURL = (NSURL *)[info valueForKey:UIImagePickerControllerReferenceURL];
    // define the block to call when we get the asset based on the url (below)
    ALAssetsLibraryAssetForURLResultBlock resultblock = ^(ALAsset *imageAsset)
    {
        ALAssetRepresentation *imageRep = [imageAsset defaultRepresentation];
        NSString* directory = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory,
                                                                  NSUserDomainMask, YES)[0];
        NSString *dest = [NSString stringWithFormat:@"%@/%@",directory,@"mservice/dest"];
        if (![[NSFileManager defaultManager] fileExistsAtPath:dest]){
            // Create folder if not exists
            [[NSFileManager defaultManager] createDirectoryAtPath:dest withIntermediateDirectories:YES attributes:nil error:nil];
        }
        NSString *destination = [NSString stringWithFormat:@"%@/%@", dest, [imageRep filename]];
        UIImage *img = ivPickedImage.image;
        NSData * data = UIImagePNGRepresentation(img);
        long imgSize = data.length;
        NSString *strImageSize = [NSString stringWithFormat:@"%ld", imgSize];
        NSString *extension = [[NSString stringWithFormat:@".%@",[destination pathExtension]] lowercaseString];
        [data writeToFile:destination atomically:YES];
        NSMutableDictionary * dict = [[NSMutableDictionary alloc] init];
        [dict setValue:destination forKey:@"filePath"];
        [dict setValue:[imageRep filename] forKey:@"fileName"];
        [dict setValue:strImageSize forKey:@"fileSize"];
        [dict setValue:extension forKey:@"fileExtension"];
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dict];
        [self.commandDelegate sendPluginResult:result callbackId:self.callbackIdForImagePicker];
    };
    // get the asset library and fetch the asset based on the ref url (pass in block above)
    NSLog(@"refURL : %@", refURL);
    ALAssetsLibrary* assetslibrary = [[ALAssetsLibrary alloc] init];
    [assetslibrary assetForURL:refURL resultBlock:resultblock failureBlock:nil];
}

-(void)imagePickerControllerDidCancel:(UIImagePickerController *)picker
{
    [picker dismissViewControllerAnimated:YES completion:nil];
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    [self.commandDelegate sendPluginResult:result callbackId:self.callbackIdForImagePicker];
}

- (void)didReceiveMemoryWarning
{
    // Dispose of any resources that can be recreated.
}

//For App update version
- (void)UpdateChoice:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        @try {
            self.callbackIdForAppUpdate = command.callbackId;
            NSMutableDictionary * dict = [[command arguments] objectAtIndex:0];
            NSString *message = [NSString stringWithFormat:@"Your mservice version is %@. There is an updated version  %@.%@ available. Please update.", dict[@"appVersion"], dict[@"softwareProductVersion"], dict[@"softwareProductSubVersion"]];
           UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"New Update"
                                                            message:message
                                                           delegate:self
                                                  cancelButtonTitle:@"Not Now"
                                                  otherButtonTitles:@"Update", nil];
            [alert show];
        } @catch (NSException *exception) {
            NSLog(@"Exception : %@", exception.description);
        }
    }];
}

- (void)UpdateConfirm:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        @try {
            self.callbackIdForAppUpdate = command.callbackId;
            NSMutableDictionary * dict = [[command arguments] objectAtIndex:0];
            NSString *message = [NSString stringWithFormat:@"Your mservice version is %@. There is an updated version  %@.%@ available. Please update.", dict[@"appVersion"], dict[@"softwareProductVersion"], dict[@"softwareProductSubVersion"]];
            UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"New Update"
                                                            message:message
                                                           delegate:self
                                                  cancelButtonTitle:nil
                                                  otherButtonTitles:@"Update Now", nil];
            [alert show];
        } @catch (NSException *exception) {
            NSLog(@"Exception : %@", exception.description);
        }
    }];
}

#pragma mark - Alert view delegate
- (void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex
{
    if(buttonIndex == [alertView cancelButtonIndex]){
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:result callbackId:self.callbackIdForAppUpdate];
        NSLog(@"Cancel button clicked.");
    } else {
        NSString *iTunesLink = @"https://itunes.apple.com/us/app/mservice/id945991789?mt=8";
        [[UIApplication sharedApplication] openURL:[NSURL URLWithString:iTunesLink]];
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        [self.commandDelegate sendPluginResult:result callbackId:self.callbackIdForAppUpdate];
    }
}

@end
