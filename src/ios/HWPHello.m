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
    
    CDVPluginResult *result = nil;
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
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (void)SendLocation:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
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
                     // generates an autoreleased NSURLConnection
                     [NSURLConnection connectionWithRequest:request delegate:self];
                 }
             }];
        }
    }];
}
- (void)GetLocation:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        NSString *docdir = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
        NSString *filePath = [NSString stringWithFormat:@"%@%@",docdir,@"/mservice/LastKnownLocation.txt"];
        NSError *error;
        NSString *locationData = [NSString stringWithContentsOfFile:filePath encoding:NSUTF8StringEncoding error:&error];
        NSArray *mySplit = [locationData componentsSeparatedByString:@","];
        NSString *locationString = [NSString stringWithFormat:@"{\"lat\":\"%@\",\"lon\":\"%@\"}", [mySplit objectAtIndex:0], [mySplit objectAtIndex:1]];
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:locationString];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }];
}

- (void)timeReader:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
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
    }];
}

- (void)CheckSumIndicatorResult:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
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
            // generates an autoreleased NSURLConnection
            [NSURLConnection connectionWithRequest:request delegate:self];
        }
    }];
}

- (void)CheckLocation:(CDVInvokedUrlCommand*)command
{
    BOOL isEnabled = false;
    if([CLLocationManager locationServicesEnabled] &&
       [CLLocationManager authorizationStatus] != kCLAuthorizationStatusDenied)
    {
        isEnabled = true;
    } else {
        isEnabled = false;
    }
    NSString *serviceResult = [NSString stringWithFormat:@"%s", isEnabled ? "true" : "false"];
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:serviceResult];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

+(BOOL)CopyFile:(NSString *)source toDestination:(NSString *)destination{
    NSString* directory = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory,
                                                              NSUserDomainMask, YES)[0];
    source = [source initWithFormat:@"%@/%@",directory,source];
    destination = [destination initWithFormat:@"%@/%@",directory,destination];
    if ( [[NSFileManager defaultManager] isReadableFileAtPath:source] )
        return [[NSFileManager defaultManager] copyItemAtURL:[NSURL URLWithString:source] toURL:[NSURL URLWithString:destination] error:nil];
    
    return false;
}

- (void)DespatchQueue:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        NSString *docdir = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
        NSString *queueFilePath = [NSString stringWithFormat:@"%@/mservice/database/queue_mgr.txt", docdir];
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
            NSString *subKeyValue = dict[@"subkey"];
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
                    NSString *backupDataObj =[NSString stringWithContentsOfFile:backupFilePath encoding:NSUTF8StringEncoding error:nil];
                    NSData *dataBackupDataObj = [backupDataObj dataUsingEncoding:NSUTF8StringEncoding];
                    NSMutableDictionary *dictBackupDataObj = [NSJSONSerialization JSONObjectWithData:dataBackupDataObj options:NSJSONReadingMutableContainers error:&jsonError];
                    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:dictBackupDataObj options:NSJSONWritingPrettyPrinted error:nil];
                    NSMutableDictionary *dictBackupDataObj111 = [NSJSONSerialization JSONObjectWithData:jsonData options:NSJSONReadingMutableContainers error:&jsonError];
                    bckpDataFullContent = dictBackupDataObj111;
                } else {
                    //make empty JSON
                    NSMutableDictionary * blankJSON = [[NSMutableDictionary alloc] init];
                    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:blankJSON options:NSJSONWritingPrettyPrinted error:nil];
                    NSMutableDictionary *dictBackupDataObj111 = [NSJSONSerialization JSONObjectWithData:jsonData options:NSJSONReadingMutableContainers error:&jsonError];
                    bckpDataFullContent = dictBackupDataObj111;
                }
                //write response to backup file where subkey matches in queue mngr file.
                bckpDataFullContent[subKeyValue] = responseString;
                NSData *fileContents = [NSJSONSerialization dataWithJSONObject:bckpDataFullContent options:NSJSONWritingPrettyPrinted error:nil];
                [fileContents writeToFile:backupFilePath atomically:true];
                // generates an autoreleased NSURLConnection
                [NSURLConnection connectionWithRequest:request delegate:self];
            } else {
                if([fileType isEqualToString:@"file"]){
                    NSString *requestFilePath = [NSString stringWithFormat:@"%@/%@/%@", docdir, sendFileBasePath, sendFileName];
                    NSLog(@"%@", requestFilePath);
                    /*UIImage *yourImage= [UIImage imageNamed:requestFilePath];
                    NSData *imageData = UIImagePNGRepresentation(yourImage);
                    NSString *postLength = [NSString stringWithFormat:@"%lu", (unsigned long)[imageData length]];
                    
                    // Init the URLRequest
                    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] init];
                    [request setHTTPMethod:@"POST"];
                    [request setURL:[NSURL URLWithString:requestUrl]];
                    //[request setValue:@"application/x-www-form-urlencoded" forHTTPHeaderField:@"Content-Type"];
                    [request setValue:@"multipart/form-data" forHTTPHeaderField:@"ENCTYPE"];
                    [request setValue:@"multipart/form-data;boundary=*****" forHTTPHeaderField:@"Content-Type"];
                    [request setValue:postLength forHTTPHeaderField:@"Content-Length"];
                    [request setHTTPBody:imageData];
                    
                    NSURLSession *session = [NSURLSession sharedSession];
                    NSURLSessionDataTask *dataTask = [session dataTaskWithRequest:request
                                                                completionHandler:^(NSData *data, NSURLResponse *response, NSError *error)
                                                      {
                                                          NSLog(@"%@",response);
                                                          NSLog(@"%@",error);
                                                          // do something with the data
                                                      }];
                    [dataTask resume];*/
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
    }];
}

@end
