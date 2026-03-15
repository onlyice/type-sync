import AppKit
import Foundation

class AppDelegate: NSObject, NSApplicationDelegate {
    private let statusBar = StatusBarController()
    private let server = WebSocketServer(port: 9876)
    private let messageHandler = MessageHandler()
    private let bonjourService = BonjourService()

    func applicationDidFinishLaunching(_ notification: Notification) {
        statusBar.setup()

        server.onMessageReceived = { [weak self] text in
            self?.messageHandler.handle(text)
        }

        server.onClientConnected = { [weak self] in
            self?.statusBar.setConnected(true)
        }

        server.onClientDisconnected = { [weak self] in
            self?.statusBar.setConnected(false)
        }

        // Wire up server→client messaging (for clipboard pull responses)
        messageHandler.onSendToClient = { [weak self] text in
            self?.server.sendToAll(text)
        }

        DispatchQueue.global().async { [weak self] in
            guard let self = self else { return }
            do {
                try self.server.start()
            } catch {
                print("Failed to start server: \(error)")
            }
        }

        bonjourService.publish(port: 9876)

        print("TypeSync Server running on port 9876")
        print("⚠️  Make sure Accessibility permissions are granted in System Settings > Privacy & Security > Accessibility")
    }

    func applicationWillTerminate(_ notification: Notification) {
        server.stop()
        bonjourService.stop()
    }
}

let app = NSApplication.shared
let delegate = AppDelegate()
app.delegate = delegate
app.setActivationPolicy(.accessory)
app.run()
