module.exports = class Trace
  constructor: (@repl, subs) ->
    code = @getFile("~/.atom/packages/clojure-plus/lib/clj/tracing.clj")
    @repl.clear()
    console.log(code)
    @repl.syncRun(code).then (e) =>
      console.log("SYNC", e)

    subs.add atom.commands.add 'atom-text-editor', 'clojure-plus:add-trace-to-function', =>
      editor = atom.workspace.getActiveTextEditor()
      varName = editor.getWordUnderCursor(wordRegex: /[a-zA-Z0-9\-.$!?:\/><\+*]+/)
      @traceFn(varName)

    subs.add atom.commands.add 'atom-text-editor', 'clojure-plus:run-and-trace', =>

    subs.add atom.commands.add 'atom-text-editor', 'clojure-plus:run-and-trace', =>
      @reset()
      editor = atom.workspace.getActiveTextEditor()
      range = protoRepl.EditorUtils.getCursorInBlockRange(editor, topLevel: true)
      protoRepl.clearRepl() if atom.config.get('clojure-plus.clearRepl')
      @repl.syncRun(editor.getTextInRange(range)).then (res) =>
        console.log("RES", res)
        @showTrace(editor, range)

  traceFn: (name) ->
    code = "(com.billpiel.sayid.core/ws-add-trace-fn! #{name})"
    @repl.syncRun(code)

  reset: ->
    code = "(clj.__tracing__/reset-sayid!)"
    @repl.syncRun(code, "_clj._sayid", session: 'exception').then (e) => console.log e

  showTrace: (editor, range) ->
    result = new protoRepl.ink.Result(editor, [range.start.row, range.end.row], type: "block")
    result.view.classList.add('proto-repl')

    @repl.syncRun("(clj.__tracing__/trace-str)").then (res) =>
      console.log("RES Trace", res)
      return unless res.value
      value = protoRepl.parseEdn(res.value)
      return unless value

      @createElements(value, result)

  createElements: (values, result) ->
    d = document.createElement('div')
    els = values.forEach ({fn, args, children, id, mapping, returned}) =>
      fn = "(#{fn} #{args.join(" ")})" if !fn.startsWith('(') && args?
      fnHTML = document.createElement('strong')
      fnHTML.innerText = "#{fn} => #{returned}"

      mappingHTML = document.createElement('div')
      for k, v of mapping
        p = document.createElement('div')
        p.innerText = "#{k} => #{v}"
        mappingHTML.appendChild(p)

      returnedHTML = document.createElement('div')
      returnedHTML.innerText = "RETURNED => #{returned}"

      childrenHTML = @createElements(children, document.createElement('div')) if children
      if !childrenHTML?
        a = document.createElement('a')
        a.innerText = "TRACE"
        a.onclick = =>
          @repl.syncRun("(clj.__tracing__/trace-inner :#{id})").then (res) =>
            childrenHTML.innerHTML = ""
            return unless res.value
            value = protoRepl.parseEdn(res.value)
            return unless value
            @createElements(value, childrenHTML)
        childrenHTML = document.createElement('div')
        childrenHTML.appendChild(a)

      child = [mappingHTML, childrenHTML, returnedHTML]
      treeHtml = protoRepl.ink.tree.treeView(fnHTML, child, {})
      d.appendChild(treeHtml)

    if result.setContent
      result.setContent(d)
    else
      result.appendChild(d)

  getFile: (file) ->
    home = process.env.HOME
    fileName = file.replace("~", home)
    fs.readFileSync(fileName).toString()
