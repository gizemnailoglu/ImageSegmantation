import * as functions from 'firebase-functions';
import * as admin from "firebase-admin";
import * as Path from "path"
import * as Os from "os";
import * as Fs from "fs";
const Spawn = require('child-process-promise').spawn;
const UUID = require("uuid-v4");
const replaceString = require('replace-string');
admin.initializeApp(); 


exports.ImageCompress = functions.https.onCall( async (data) => {   
    const compRate =  data.compRate
    const filePathName = data.path_name

    const bucket =admin.storage().bucket(admin.storage().bucket().name);   
    const uuid=UUID();
    const fileName = Path.basename(filePathName);
    const tempFilePath = Path.join(Os.tmpdir(), fileName);

    await bucket.file(filePathName).download({ destination: tempFilePath });

    const orjinalFileSize = Fs.statSync(tempFilePath).size/1024
 
    await Spawn('convert', [  tempFilePath,                            
                            '-quality', compRate,
                            tempFilePath]);
  
    
    const compFileName = `comp_${fileName}`;
    const compFilePath = Path.join(Path.dirname(filePathName), compFileName);    

    return bucket.upload(tempFilePath, {
        destination: compFilePath          
    }).then(()=>{      
        const resultUrl = "https://firebasestorage.googleapis.com/v0/b/"+bucket.name+"/o/"+replaceString(compFilePath,"/","%2F")+"?alt=media&token="+uuid  
        console.log(resultUrl)
        const fileSize =Fs.statSync(tempFilePath).size/1024
       // console.log(fileSize)
        Fs.unlinkSync(tempFilePath); 
        return {
            result:resultUrl,
            rate :compRate,
            fileSize:fileSize,
            orjinalFileSize:orjinalFileSize
        }
    }).catch(()=>{
        return{
            result:"UPSS",
            rate:compRate
        }
    });    
  
});

exports.ImageSegmantation = functions.https.onCall( async (data) => {   
    const filePathName = data.path_name
    const bucket =admin.storage().bucket(admin.storage().bucket().name);  
    const fileName = Path.basename(filePathName);
    const tempFilePath = Path.join(Os.tmpdir(), fileName);
    
    await bucket.file(filePathName).download({ destination: tempFilePath });
    const vision = require('@google-cloud/vision');
    const client = new vision.ImageAnnotatorClient();   
    const [result] = await client.objectLocalization(tempFilePath);
    const objects = result.localizedObjectAnnotations;   
    return {
        result: objects,           
    };   

});



