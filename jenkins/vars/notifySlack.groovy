def call(Map config = [:]) {
    def is_success = config.is_success ?: false
    def message = is_success ? 'Build succeeded!' : 'Build failed!'
    def channel = config.channel ?: '#build'
    def color = is_success ? '#00FF00' : '#FF0000'

    slackSend(channel: channel, color: color, message: message)

}
