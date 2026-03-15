import Foundation

enum MessageType: String, Codable {
    case text
    case key
    case macro
    case clipboard
    case mouse
}

struct KeyModifier: OptionSet, Codable {
    let rawValue: Int
    static let cmd = KeyModifier(rawValue: 1 << 0)
    static let ctrl = KeyModifier(rawValue: 1 << 1)
    static let alt = KeyModifier(rawValue: 1 << 2)
    static let shift = KeyModifier(rawValue: 1 << 3)
}

struct Message: Codable {
    let type: MessageType
    // text
    var content: String?
    // key
    var key: String?
    var modifiers: [String]?
    // macro
    var name: String?
    // clipboard
    var action: String?
    // mouse
    var dx: Double?
    var dy: Double?
    var button: String?
}
