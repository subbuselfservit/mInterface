#import "HWPHello.h"
#import "XMLConverter.h"
#import "InternetConnection.h"

@implementation HWPHello

- (void)StartService:(CDVInvokedUrlCommand*)command
{
   CDVPluginResult *result = nil;
   NSTimer *timer = [NSTimer scheduledTimerWithTimeInterval:20.0
                                                  target:self 
                                                selector:@selector(getLastKnownLocation:) 
                                                userInfo:nil 
                                                 repeats:YES];
   [[NSRunLoop currentRunLoop] addTimer:timer forMode:NSRunLoopCommonModes];
       /* [NSTimer scheduledTimerWithTimeInterval:20.0
                                         target:self
                                       selector:@selector(SendLocation)
                                       userInfo:nil
                                        repeats:YES];*/
    
   result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
   [result setKeepCallbackAsBool:YES];
   [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (void)viewDidLoad {
    [self.commandDelegate runInBackground:^{
    [NSTimer scheduledTimerWithTimeInterval:10.0
                                     target:self
                                   selector:@selector(GetCurrentLocation)
                                   userInfo:nil
                                    repeats:YES];
     }];
    // Do any additional setup after loading the view, typically from a nib.
}

- (void)GetCurrentLocation{
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
            NSLog(@"File is not there so created once");
        }
        NSLog(@"%@",fullPath);

        double lat = [Utils sharedSingleton].locationManager.location.coordinate.latitude;
        double lngt = [Utils sharedSingleton].locationManager.location.coordinate.longitude;
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
        
        NSLog(@"Text successfully written..");
        NSLog(@"Latitude : %f", lat);
        NSLog(@"Longitude : %f", lngt);
        NSLog(@"%@", [dateFormatter stringFromDate:[NSDate date]]);
    }];
}

- (void)SendLocation
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
   CDVPluginResult *result = nil;
   NSTimer *timer = [NSTimer scheduledTimerWithTimeInterval:20.0
                                                  target:self 
                                                selector:@selector(pluginResultForTimer:) 
                                                userInfo:nil 
                                                 repeats:YES];
   [[NSRunLoop currentRunLoop] addTimer:timer forMode:NSRunLoopCommonModes];
    
   result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
   [result setKeepCallbackAsBool:YES];
   [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (void)pluginResultForTimer:(CDVInvokedUrlCommand*)command
{   
   CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"HI pudhiyavan"];
   [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}
@end
