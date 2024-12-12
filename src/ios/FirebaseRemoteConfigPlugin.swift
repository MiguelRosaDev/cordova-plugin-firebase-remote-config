import Foundation
//import Firebase
import FirebaseRemoteConfig
import FirebaseCore;

@objc(FirebaseRemoteConfigPlugin)
class FirebaseRemoteConfigPlugin: CDVPlugin {
    
    private var remoteConfig: RemoteConfig!
    private var cdvCommand: CDVInvokedUrlCommand?
    private var callback: String?
    
    override func pluginInitialize() {
        super.pluginInitialize()
        
        // mlrosa - Check if Firebase is already configured
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
        }
        
        remoteConfig = RemoteConfig.remoteConfig()
        
        let settings = RemoteConfigSettings()
        settings.minimumFetchInterval = 3600
        remoteConfig.configSettings = settings
        
        addOnConfigUpdateListener()
    }

    private func addOnConfigUpdateListener() {
        remoteConfig.addOnConfigUpdateListener { [weak self] configUpdate, error in
            guard error == nil else {
                if let callback = self?.callback, let self = self {
                    sendPluginResult(
                        status: CDVCommandStatus_ERROR,
                        message: "Error listening for config updates: \(String(describing: error))",
                        callbackId: callback
                    )
                }
                
                return
            }

            guard let self = self else { return }
            
            self.remoteConfig.activate { changed, error in
                guard error == nil else {
                    print("Error activating config: \(String(describing: error))")
                    return
                }

                if let command = self.cdvCommand {
                    self.getAllKeys(command: command)
                }
            }
        }
    }
    
    @objc(getAllKeys:)
    func fetchAndActivate(command: CDVInvokedUrlCommand) {
        cdvCommand = command
        callback = command.callbackId
        
        remoteConfig.fetchAndActivate { status, error in
            if status == .successFetchedFromRemote || status == .successUsingPreFetchedData {
                self.getAllKeys(command: command)
            } else {
                if self.callback != nil {
                    self.sendPluginResult(
                        status: CDVCommandStatus_ERROR,
                        message: error?.localizedDescription ?? "Unknown error",
                        callbackId: self.callback!
                    )
                }
            }
        }
    }
    
    private func getAllKeys(command: CDVInvokedUrlCommand) {
        let keys = remoteConfig.allKeys(from: .remote)
        let jsonArray = keys.map { key -> [String: Any] in
            return ["key": key, "value": remoteConfig[key].stringValue ?? ""]
        }
        
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: jsonArray)
        pluginResult?.setKeepCallbackAs(true)
        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }
    
    func sendPluginResult(status: CDVCommandStatus, message: String = "", callbackId: String, keepCallback: Bool = false) {
        let pluginResult = CDVPluginResult(status: status, messageAs: message)
        pluginResult?.setKeepCallbackAs(keepCallback)
        self.commandDelegate!.send(pluginResult, callbackId: callbackId)
    }
}
