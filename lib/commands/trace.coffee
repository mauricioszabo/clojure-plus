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
    result.view.classList.add('proto-repl')

    @repl.syncRun("(clj.__tracing__/trace-str)").then (res) =>
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
