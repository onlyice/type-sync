import AppKit
import Foundation

class StatusBarController {
    private var statusItem: NSStatusItem?
    private var isConnected = false
    private var statusMenuItem: NSMenuItem?
    private var ipMenuItem: NSMenuItem?

    func setup() {
        statusItem = NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)
        updateIcon()
        setupMenu()
    }

    func setConnected(_ connected: Bool) {
        isConnected = connected
        DispatchQueue.main.async {
            self.updateIcon()
            self.statusMenuItem?.title = connected ? "状态: 已连接" : "状态: 等待连接"
        }
    }

    private func updateIcon() {
        if let button = statusItem?.button {
            button.title = isConnected ? "⌨️✅" : "⌨️"
        }
    }

    private func setupMenu() {
        let menu = NSMenu()

        let titleItem = NSMenuItem(title: "TypeSync Server", action: nil, keyEquivalent: "")
        titleItem.isEnabled = false
        menu.addItem(titleItem)

        menu.addItem(NSMenuItem.separator())

        statusMenuItem = NSMenuItem(title: "状态: 等待连接", action: nil, keyEquivalent: "")
        statusMenuItem?.isEnabled = false
        menu.addItem(statusMenuItem!)

        // Show local IP addresses
        let ips = getLocalIPAddresses()
        let ipText = ips.isEmpty ? "IP: 未知" : "IP: " + ips.joined(separator: ", ")
        ipMenuItem = NSMenuItem(title: "\(ipText) : 9876", action: nil, keyEquivalent: "")
        ipMenuItem?.isEnabled = false
        menu.addItem(ipMenuItem!)

        menu.addItem(NSMenuItem.separator())

        let copyIPItem = NSMenuItem(title: "复制 IP 地址", action: #selector(copyIP), keyEquivalent: "c")
        copyIPItem.target = self
        menu.addItem(copyIPItem)

        menu.addItem(NSMenuItem.separator())

        let quitItem = NSMenuItem(title: "Quit", action: #selector(NSApplication.terminate(_:)), keyEquivalent: "q")
        menu.addItem(quitItem)

        statusItem?.menu = menu
    }

    @objc private func copyIP() {
        let ips = getLocalIPAddresses()
        if let ip = ips.first {
            NSPasteboard.general.clearContents()
            NSPasteboard.general.setString(ip, forType: .string)
        }
    }

    private func getLocalIPAddresses() -> [String] {
        var addresses: [String] = []
        var ifaddr: UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&ifaddr) == 0, let firstAddr = ifaddr else { return addresses }
        defer { freeifaddrs(ifaddr) }

        var ptr = firstAddr
        while true {
            let interface = ptr.pointee
            let addrFamily = interface.ifa_addr.pointee.sa_family

            if addrFamily == UInt8(AF_INET) { // IPv4
                let name = String(cString: interface.ifa_name)
                if name == "en0" || name == "en1" { // Wi-Fi / Ethernet
                    var hostname = [CChar](repeating: 0, count: Int(NI_MAXHOST))
                    getnameinfo(
                        interface.ifa_addr,
                        socklen_t(interface.ifa_addr.pointee.sa_len),
                        &hostname,
                        socklen_t(hostname.count),
                        nil, 0,
                        NI_NUMERICHOST
                    )
                    addresses.append(String(cString: hostname))
                }
            }

            guard let next = interface.ifa_next else { break }
            ptr = next
        }
        return addresses
    }
}
