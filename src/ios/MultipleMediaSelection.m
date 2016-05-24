//
//  MultipleMediaSelection.m
//

#import "MultipleMediaSelection.h"
#import <ImageIO/ImageIO.h>
#import <MobileCoreServices/MobileCoreServices.h>

#define CDV_PHOTO_PREFIX @"cdv_photo_"

@interface MultipleMediaSelection ()

@property (copy) NSString* callbackId;

@end

@implementation MultipleMediaSelection
@synthesize callbackId;

- (void) getPictures:(CDVInvokedUrlCommand *)command {
    NSDictionary *options = [command.arguments objectAtIndex: 0];
    [self.commandDelegate runInBackground:^{
        NSInteger maxImages = [options[@"maxImages"] integerValue];
        NSInteger minImages = [options[@"minImages"] integerValue];
        BOOL sharedAlbums = [options[@"sharedAlbums"] boolValue] ?: false;
        NSString *mediaType = (NSString *)options[@"mediaType"];
        
        // Create the an album controller and image picker
        QBImagePickerController *imagePicker = [[QBImagePickerController alloc] init];
        imagePicker.allowsMultipleSelection = (maxImages >= 2);
        imagePicker.showsNumberOfSelectedAssets = YES;
        imagePicker.maximumNumberOfSelection = maxImages;
        imagePicker.minimumNumberOfSelection = minImages;
        
        NSMutableArray *collections = [imagePicker.assetCollectionSubtypes mutableCopy];
        if (sharedAlbums) {
            [collections addObject:@(PHAssetCollectionSubtypeAlbumCloudShared)];
        }
        
        if ([mediaType isEqualToString:@"image"]) {
            imagePicker.mediaType = QBImagePickerMediaTypeImage;
            [collections removeObject:@(PHAssetCollectionSubtypeSmartAlbumVideos)];
        } else if ([mediaType isEqualToString:@"video"]) {
            imagePicker.mediaType = QBImagePickerMediaTypeVideo;
        } else {
            imagePicker.mediaType = QBImagePickerMediaTypeAny;
        }
        
        imagePicker.assetCollectionSubtypes = collections;
        
        imagePicker.delegate = self;
        self.callbackId = command.callbackId;
        
        // Display the picker in the main thread.
        __weak MultipleMediaSelection* weakSelf = self;
        dispatch_async(dispatch_get_main_queue(), ^{
            [weakSelf.viewController presentViewController:imagePicker animated:YES completion:nil];
        });
    }];
}

- (NSString*)getAssetFilePath:(NSString*)extension
{
    NSArray* paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString* docsPath = ([paths count] > 0) ? [paths objectAtIndex:0] : nil;
    
    NSFileManager* fileMgr = [[NSFileManager alloc] init]; // recommended by Apple (vs [NSFileManager defaultManager]) to be threadsafe
    NSString* filePath;
    
    // generate unique file name
    int i = 1;
    do {
        filePath = [NSString stringWithFormat:@"%@/%@%05d.%@", docsPath, CDV_PHOTO_PREFIX, i++, extension];
    } while ([fileMgr fileExistsAtPath:filePath]);
    
    return filePath;
}

#pragma mark - QBImagePickerControllerDelegate
- (void)qb_imagePickerController:(QBImagePickerController *)imagePickerController didFinishPickingAssets:(NSArray *)assets
{
    NSLog(@"Selected assets:");
    NSLog(@"%@", assets);
    PHImageManager *manager = [PHImageManager defaultManager];
    PHImageRequestOptions *options = [[PHImageRequestOptions alloc] init];
    options.synchronous = YES;
    
    __block NSMutableArray *resultStrings = [[NSMutableArray alloc] init];
    
    for (PHAsset *asset in assets) {
        if (asset.mediaType == PHAssetMediaTypeImage) {
            [manager requestImageDataForAsset: asset options: options resultHandler:^(NSData *imageData, NSString *dataUTI, UIImageOrientation orientation, NSDictionary *info) {
                NSString *filePath = [self getAssetFilePath:@"jpg"];
                NSURL *fileURL = [NSURL fileURLWithPath:filePath isDirectory:NO];
                [imageData writeToFile:filePath atomically:YES];
                [resultStrings addObject:[fileURL absoluteString]];
                if ([resultStrings count] == [assets count]) {
                    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:resultStrings];
                    [self didFinishImagesWithResult:pluginResult];
                }
            }];
        } else if (asset.mediaType == PHAssetMediaTypeVideo) {
            dispatch_semaphore_t sem = dispatch_semaphore_create(0);
            [manager requestAVAssetForVideo:asset options:nil resultHandler:^(AVAsset *videoAsset, AVAudioMix *audioMix, NSDictionary *info) {
                if ([videoAsset isKindOfClass:[AVURLAsset class]])
                {
                    NSString *filePath = [self getAssetFilePath:@"mp4"];
                    NSURL *fileURL = [NSURL fileURLWithPath:filePath isDirectory:NO];
                    
                    NSURL *inputURL = [(AVURLAsset*)videoAsset URL];
                    NSData *videoData = [NSData dataWithContentsOfURL:inputURL];
                    
                    [videoData writeToFile: filePath atomically:YES];
                    
                    [resultStrings addObject:[fileURL absoluteString]];
                    if ([resultStrings count] == [assets count]) {
                        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:resultStrings];
                        [self didFinishImagesWithResult:pluginResult];
                    }
                }
                dispatch_semaphore_signal(sem);
            }];
            dispatch_semaphore_wait(sem, DISPATCH_TIME_FOREVER);
        } else {
            CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Unhandled Asset Type."];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
            self.callbackId = nil;
        }
    }
    
    __weak MultipleMediaSelection* weakSelf = self;
    [weakSelf.viewController dismissViewControllerAnimated:YES completion:NULL];
}

- (void) didFinishImagesWithResult: (CDVPluginResult *)pluginResult
{
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
    self.callbackId = nil;
}

- (void)qb_imagePickerControllerDidCancel:(QBImagePickerController *)imagePickerController
{
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"User cancelled."];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
    self.callbackId = nil;
    
    __weak MultipleMediaSelection* weakSelf = self;
    [weakSelf.viewController dismissViewControllerAnimated:YES completion:NULL];
}
@end

