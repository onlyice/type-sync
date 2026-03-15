import Foundation
import NIOCore
import NIOPosix
import NIOHTTP1
import NIOWebSocket

class WebSocketServer {
    private var group: EventLoopGroup?
    private var serverChannel: Channel?
    private var connectedHandlers: [ObjectIdentifier: WebSocketFrameHandler] = [:]
    private let lock = NSLock()

    var onMessageReceived: ((String) -> Void)?
    var onClientConnected: (() -> Void)?
    var onClientDisconnected: (() -> Void)?

    let port: Int

    init(port: Int = 9876) {
        self.port = port
    }

    func start() throws {
        let group = MultiThreadedEventLoopGroup(numberOfThreads: 2)
        self.group = group

        let upgrader = NIOWebSocketServerUpgrader(
            shouldUpgrade: { channel, head in
                channel.eventLoop.makeSucceededFuture(HTTPHeaders())
            },
            upgradePipelineHandler: { [weak self] channel, req in
                let handler = WebSocketFrameHandler()
                let handlerID = ObjectIdentifier(handler)

                handler.onText = { text in
                    self?.onMessageReceived?(text)
                }
                handler.onConnected = {
                    self?.lock.lock()
                    self?.connectedHandlers[handlerID] = handler
                    self?.lock.unlock()
                    self?.onClientConnected?()
                }
                handler.onDisconnected = {
                    self?.lock.lock()
                    self?.connectedHandlers.removeValue(forKey: handlerID)
                    self?.lock.unlock()
                    self?.onClientDisconnected?()
                }
                return channel.pipeline.addHandler(handler)
            }
        )

        let bootstrap = ServerBootstrap(group: group)
            .serverChannelOption(.backlog, value: 256)
            .serverChannelOption(.socketOption(.so_reuseaddr), value: 1)
            .childChannelInitializer { channel in
                let config: NIOHTTPServerUpgradeConfiguration = (
                    upgraders: [upgrader],
                    completionHandler: { ctx in }
                )
                return channel.pipeline.configureHTTPServerPipeline(
                    withServerUpgrade: config
                )
            }

        self.serverChannel = try bootstrap.bind(host: "0.0.0.0", port: port).wait()
        print("WebSocket server started on port \(port)")
    }

    func sendToAll(_ text: String) {
        lock.lock()
        let handlers = Array(connectedHandlers.values)
        lock.unlock()
        for handler in handlers {
            handler.send(text: text)
        }
    }

    func stop() {
        try? serverChannel?.close().wait()
        try? group?.syncShutdownGracefully()
    }
}

// MARK: - WebSocket Frame Handler

class WebSocketFrameHandler: ChannelInboundHandler {
    typealias InboundIn = WebSocketFrame
    typealias OutboundOut = WebSocketFrame

    var onText: ((String) -> Void)?
    var onConnected: (() -> Void)?
    var onDisconnected: (() -> Void)?

    private var context: ChannelHandlerContext?
    private var awaitingClose = false

    func send(text: String) {
        guard let context = context else { return }
        var buffer = context.channel.allocator.buffer(capacity: text.utf8.count)
        buffer.writeString(text)
        let frame = WebSocketFrame(fin: true, opcode: .text, data: buffer)
        context.writeAndFlush(wrapOutboundOut(frame), promise: nil)
    }

    func channelActive(context: ChannelHandlerContext) {
        self.context = context
        onConnected?()
    }

    func channelInactive(context: ChannelHandlerContext) {
        self.context = nil
        onDisconnected?()
    }

    func channelRead(context: ChannelHandlerContext, data: NIOAny) {
        let frame = unwrapInboundIn(data)

        switch frame.opcode {
        case .text:
            var data = frame.unmaskedData
            if let text = data.readString(length: data.readableBytes) {
                onText?(text)
            }
        case .connectionClose:
            if awaitingClose {
                context.close(promise: nil)
            } else {
                var frameData = context.channel.allocator.buffer(capacity: 2)
                frameData.write(webSocketErrorCode: .normalClosure)
                let closeFrame = WebSocketFrame(fin: true, opcode: .connectionClose, data: frameData)
                context.writeAndFlush(wrapOutboundOut(closeFrame)).whenComplete { _ in
                    context.close(promise: nil)
                }
            }
        case .ping:
            let pongFrame = WebSocketFrame(fin: true, opcode: .pong, data: frame.unmaskedData)
            context.writeAndFlush(wrapOutboundOut(pongFrame), promise: nil)
        default:
            break
        }
    }

    func errorCaught(context: ChannelHandlerContext, error: Error) {
        print("WebSocket error: \(error)")
        context.close(promise: nil)
    }
}
