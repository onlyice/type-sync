import AppKit
import Foundation
import CoreGraphics

class MessageHandler {
    private let keySimulator = KeySimulator()
    var onSendToClient: ((String) -> Void)?

    func handle(_ jsonString: String) {
        guard let data = jsonString.data(using: .utf8) else { return }

        do {
            let message = try JSONDecoder().decode(Message.self, from: data)

            switch message.type {
            case .text:
                if let content = message.content {
                    keySimulator.typeText(content)
                }
            case .key:
                if let key = message.key {
                    keySimulator.sendKey(key, modifiers: message.modifiers ?? [])
                }
            case .macro:
                if let name = message.name {
                    keySimulator.executeMacro(name)
                }
            case .clipboard:
                handleClipboard(message)
            case .mouse:
                handleMouse(message)
            }
        } catch {
            print("Failed to decode message: \(error)")
        }
    }

    private func handleClipboard(_ message: Message) {
        guard let action = message.action else { return }

        switch action {
        case "push":
            if let content = message.content {
                DispatchQueue.main.async {
                    NSPasteboard.general.clearContents()
                    NSPasteboard.general.setString(content, forType: .string)
                    print("Clipboard received from client: \(content.prefix(50))...")
                }
            }
        case "pull":
            DispatchQueue.main.async { [weak self] in
                let content = NSPasteboard.general.string(forType: .string) ?? ""
                let response: [String: String] = [
                    "type": "clipboard",
                    "action": "content",
                    "content": content
                ]
                if let jsonData = try? JSONSerialization.data(withJSONObject: response),
                   let jsonString = String(data: jsonData, encoding: .utf8) {
                    self?.onSendToClient?(jsonString)
                }
            }
        default:
            break
        }
    }

    private func handleMouse(_ message: Message) {
        guard let action = message.action else { return }

        switch action {
        case "move":
            guard let dx = message.dx, let dy = message.dy else { return }
            let currentPos = NSEvent.mouseLocation
            let screenHeight = NSScreen.main?.frame.height ?? 1080
            let cgY = screenHeight - currentPos.y
            let newX = currentPos.x + dx
            let newY = cgY + dy
            let point = CGPoint(x: newX, y: newY)

            let moveEvent = CGEvent(mouseEventSource: nil, mouseType: .mouseMoved,
                                     mouseCursorPosition: point, mouseButton: .left)
            moveEvent?.post(tap: .cgSessionEventTap)

        case "click":
            let button = message.button ?? "left"
            let currentPos = NSEvent.mouseLocation
            let screenHeight = NSScreen.main?.frame.height ?? 1080
            let point = CGPoint(x: currentPos.x, y: screenHeight - currentPos.y)

            let mouseButton: CGMouseButton = button == "right" ? .right : .left
            let downType: CGEventType = button == "right" ? .rightMouseDown : .leftMouseDown
            let upType: CGEventType = button == "right" ? .rightMouseUp : .leftMouseUp

            let down = CGEvent(mouseEventSource: nil, mouseType: downType,
                               mouseCursorPosition: point, mouseButton: mouseButton)
            let up = CGEvent(mouseEventSource: nil, mouseType: upType,
                             mouseCursorPosition: point, mouseButton: mouseButton)
            down?.post(tap: .cgSessionEventTap)
            up?.post(tap: .cgSessionEventTap)

        case "scroll":
            let dy = Int32(message.dy ?? 0)
            let scrollEvent = CGEvent(scrollWheelEvent2Source: nil, units: .pixel,
                                       wheelCount: 1, wheel1: dy, wheel2: 0, wheel3: 0)
            scrollEvent?.post(tap: .cgSessionEventTap)

        case "drag_start":
            let currentPos = NSEvent.mouseLocation
            let screenHeight = NSScreen.main?.frame.height ?? 1080
            let point = CGPoint(x: currentPos.x, y: screenHeight - currentPos.y)
            let down = CGEvent(mouseEventSource: nil, mouseType: .leftMouseDown,
                               mouseCursorPosition: point, mouseButton: .left)
            down?.post(tap: .cgSessionEventTap)

        case "drag_move":
            guard let dx = message.dx, let dy = message.dy else { return }
            let currentPos = NSEvent.mouseLocation
            let screenHeight = NSScreen.main?.frame.height ?? 1080
            let cgY = screenHeight - currentPos.y
            let newX = currentPos.x + dx
            let newY = cgY + dy
            let point = CGPoint(x: newX, y: newY)
            let dragEvent = CGEvent(mouseEventSource: nil, mouseType: .leftMouseDragged,
                                    mouseCursorPosition: point, mouseButton: .left)
            dragEvent?.post(tap: .cgSessionEventTap)

        case "drag_end":
            let currentPos = NSEvent.mouseLocation
            let screenHeight = NSScreen.main?.frame.height ?? 1080
            let point = CGPoint(x: currentPos.x, y: screenHeight - currentPos.y)
            let up = CGEvent(mouseEventSource: nil, mouseType: .leftMouseUp,
                             mouseCursorPosition: point, mouseButton: .left)
            up?.post(tap: .cgSessionEventTap)

        case "double_click":
            let currentPos = NSEvent.mouseLocation
            let screenHeight = NSScreen.main?.frame.height ?? 1080
            let point = CGPoint(x: currentPos.x, y: screenHeight - currentPos.y)

            let down1 = CGEvent(mouseEventSource: nil, mouseType: .leftMouseDown,
                                mouseCursorPosition: point, mouseButton: .left)
            down1?.setIntegerValueField(.mouseEventClickState, value: 1)
            down1?.post(tap: .cgSessionEventTap)
            let up1 = CGEvent(mouseEventSource: nil, mouseType: .leftMouseUp,
                              mouseCursorPosition: point, mouseButton: .left)
            up1?.setIntegerValueField(.mouseEventClickState, value: 1)
            up1?.post(tap: .cgSessionEventTap)

            let down2 = CGEvent(mouseEventSource: nil, mouseType: .leftMouseDown,
                                mouseCursorPosition: point, mouseButton: .left)
            down2?.setIntegerValueField(.mouseEventClickState, value: 2)
            down2?.post(tap: .cgSessionEventTap)
            let up2 = CGEvent(mouseEventSource: nil, mouseType: .leftMouseUp,
                              mouseCursorPosition: point, mouseButton: .left)
            up2?.setIntegerValueField(.mouseEventClickState, value: 2)
            up2?.post(tap: .cgSessionEventTap)

        default:
            break
        }
    }
}
