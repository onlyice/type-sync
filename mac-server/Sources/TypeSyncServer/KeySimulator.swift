import Foundation
import CoreGraphics
import Carbon.HIToolbox

class KeySimulator {

    /// Type a Unicode string by sending CGEvents with unicode characters
    func typeText(_ text: String) {
        let source = CGEventSource(stateID: .hidSystemState)
        for scalar in text.unicodeScalars {
            var utf16 = Array(String(scalar).utf16)
            let keyDown = CGEvent(keyboardEventSource: source, virtualKey: 0, keyDown: true)
            keyDown?.keyboardSetUnicodeString(stringLength: utf16.count, unicodeString: &utf16)
            keyDown?.post(tap: .cgSessionEventTap)

            let keyUp = CGEvent(keyboardEventSource: source, virtualKey: 0, keyDown: false)
            keyUp?.keyboardSetUnicodeString(stringLength: utf16.count, unicodeString: &utf16)
            keyUp?.post(tap: .cgSessionEventTap)

            usleep(5000)
        }
    }

    /// Send a key press with optional modifiers
    func sendKey(_ keyName: String, modifiers: [String]) {
        guard let keyCode = keyCodeMap[keyName.lowercased()] else {
            if keyName.count == 1 && modifiers.isEmpty {
                typeText(keyName)
                return
            }
            if keyName.count == 1, let code = charToKeyCode(keyName) {
                sendKeyCode(code, modifiers: parseModifiers(modifiers))
                return
            }
            print("Unknown key: \(keyName)")
            return
        }
        sendKeyCode(keyCode, modifiers: parseModifiers(modifiers))
    }

    /// Execute a named macro
    func executeMacro(_ name: String) {
        switch name {
        case "clear_input":
            sendKeyCode(keyCodeMap["a"]!, modifiers: .maskCommand)
            usleep(50000)
            sendKeyCode(keyCodeMap["backspace"]!, modifiers: [])
        case "select_all_copy":
            sendKeyCode(keyCodeMap["a"]!, modifiers: .maskCommand)
            usleep(50000)
            sendKeyCode(keyCodeMap["c"]!, modifiers: .maskCommand)
        case "select_all_cut":
            sendKeyCode(keyCodeMap["a"]!, modifiers: .maskCommand)
            usleep(50000)
            sendKeyCode(keyCodeMap["x"]!, modifiers: .maskCommand)
        case "select_all_paste":
            sendKeyCode(keyCodeMap["a"]!, modifiers: .maskCommand)
            usleep(50000)
            sendKeyCode(keyCodeMap["v"]!, modifiers: .maskCommand)
        case "undo":
            sendKeyCode(keyCodeMap["z"]!, modifiers: .maskCommand)
        case "redo":
            sendKeyCode(keyCodeMap["z"]!, modifiers: [.maskCommand, .maskShift])
        case "line_delete":
            // Cmd+Shift+K (works in most code editors)
            sendKeyCode(keyCodeMap["k"]!, modifiers: [.maskCommand, .maskShift])
        case "line_select":
            // Home then Shift+End
            sendKeyCode(keyCodeMap["home"]!, modifiers: [])
            usleep(30000)
            sendKeyCode(keyCodeMap["end"]!, modifiers: .maskShift)
        case "word_delete_back":
            // Alt+Backspace — delete word backwards
            sendKeyCode(keyCodeMap["backspace"]!, modifiers: .maskAlternate)
        case "word_delete_forward":
            // Alt+Delete — delete word forwards
            sendKeyCode(keyCodeMap["delete"]!, modifiers: .maskAlternate)
        case "screenshot":
            // Cmd+Shift+4
            sendKeyCode(keyCodeMap["4"]!, modifiers: [.maskCommand, .maskShift])
        case "spotlight":
            // Cmd+Space
            sendKeyCode(keyCodeMap["space"]!, modifiers: .maskCommand)
        default:
            print("Unknown macro: \(name)")
        }
    }

    // MARK: - Private

    private func sendKeyCode(_ keyCode: CGKeyCode, modifiers: CGEventFlags) {
        let source = CGEventSource(stateID: .hidSystemState)
        let keyDown = CGEvent(keyboardEventSource: source, virtualKey: keyCode, keyDown: true)
        let keyUp = CGEvent(keyboardEventSource: source, virtualKey: keyCode, keyDown: false)

        if !modifiers.isEmpty {
            keyDown?.flags = modifiers
            keyUp?.flags = modifiers
        }

        keyDown?.post(tap: .cgSessionEventTap)
        keyUp?.post(tap: .cgSessionEventTap)
    }

    private func parseModifiers(_ names: [String]) -> CGEventFlags {
        var flags: CGEventFlags = []
        for name in names {
            switch name.lowercased() {
            case "cmd", "command": flags.insert(.maskCommand)
            case "ctrl", "control": flags.insert(.maskControl)
            case "alt", "option": flags.insert(.maskAlternate)
            case "shift": flags.insert(.maskShift)
            default: break
            }
        }
        return flags
    }

    private func charToKeyCode(_ char: String) -> CGKeyCode? {
        let lower = char.lowercased()
        return keyCodeMap[lower]
    }

    private let keyCodeMap: [String: CGKeyCode] = [
        "a": 0x00, "b": 0x0B, "c": 0x08, "d": 0x02, "e": 0x0E,
        "f": 0x03, "g": 0x05, "h": 0x04, "i": 0x22, "j": 0x26,
        "k": 0x28, "l": 0x25, "m": 0x2E, "n": 0x2D, "o": 0x1F,
        "p": 0x23, "q": 0x0C, "r": 0x0F, "s": 0x01, "t": 0x11,
        "u": 0x20, "v": 0x09, "w": 0x0D, "x": 0x07, "y": 0x10,
        "z": 0x06,
        "0": 0x1D, "1": 0x12, "2": 0x13, "3": 0x14, "4": 0x15,
        "5": 0x17, "6": 0x16, "7": 0x1A, "8": 0x1C, "9": 0x19,
        "return": 0x24, "enter": 0x24, "tab": 0x30, "space": 0x31,
        "backspace": 0x33, "delete": 0x75, "escape": 0x35, "esc": 0x35,
        "up": 0x7E, "down": 0x7D, "left": 0x7B, "right": 0x7C,
        "home": 0x73, "end": 0x77, "pageup": 0x74, "pagedown": 0x79,
        "f1": 0x7A, "f2": 0x78, "f3": 0x63, "f4": 0x76,
        "f5": 0x60, "f6": 0x61, "f7": 0x62, "f8": 0x64,
        "f9": 0x65, "f10": 0x6D, "f11": 0x67, "f12": 0x6F,
        "-": 0x1B, "=": 0x18, "[": 0x21, "]": 0x1E,
        "\\": 0x2A, ";": 0x29, "'": 0x27, ",": 0x2B,
        ".": 0x2F, "/": 0x2C, "`": 0x32,
    ]
}
