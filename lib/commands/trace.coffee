module.exports = class Trace
  constructor: (@repl) ->
    code = @getFile("~/.atom/packages/clojure-plus/lib/clj/tracing.clj")
    @repl.clear()
    @repl.syncRun(code)

  traceFn: (name) ->
    editor = atom.workspace.getActiveTextEditor()
    varName = editor.getWordUnderCursor(wordRegex: /[a-zA-Z0-9\-.$!?:\/><\+*]+/)
    code = "(com.billpiel.sayid.core/ws-add-trace-fn! #{varName})"
    @repl.syncRun(code)

  reset: ->
    code = "(clj.__tracing__/reset-sayid!)"
    @repl.syncRun(code, "_clj._sayid", session: 'exception').then (e) => console.log e

  showTrace: ->
    editor = atom.workspace.getActiveTextEditor()
    range = editor.getSelectedBufferRange()
    result = new protoRepl.ink.Result(editor, [range.start.row, range.end.row], type: "block")
    causeHtml = document.createElement('strong')
    # causeHtml.classList.add('error-description')
    causeHtml.innerText = "FOOBAR"

    treeHtml = protoRepl.ink.tree.treeView(causeHtml, traceHtmls, {})
    result.setContent(causeHtml)
    result.view.classList.add('proto-repl')

  getFile: (file) ->
    home = process.env.HOME
    fileName = file.replace("~", home)
    fs.readFileSync(fileName).toString()
