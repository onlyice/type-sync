import Foundation

class BonjourService: NSObject, NetServiceDelegate {
    private var service: NetService?

    func publish(port: Int) {
        service = NetService(
            domain: "",
            type: "_typesync._tcp.",
            name: Host.current().localizedName ?? "Mac",
            port: Int32(port)
        )
        service?.delegate = self
        service?.publish()
    }

    func stop() {
        service?.stop()
    }

    func netServiceDidPublish(_ sender: NetService) {
        print("Bonjour service published: \(sender.name) on port \(sender.port)")
    }

    func netService(_ sender: NetService, didNotPublish errorDict: [String: NSNumber]) {
        print("Failed to publish Bonjour service: \(errorDict)")
    }
}
