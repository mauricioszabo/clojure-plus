module.exports = class Trace
  constructor: (@repl, subs) ->
    subs.add atom.commands.add 'atom-text-editor', 'clojure-plus:add-trace-to-function', =>
      editor = atom.workspace.getActiveTextEditor()
      varName = editor.getWordUnderCursor(wordRegex: /[a-zA-Z0-9\-.$!?:\/><\+*]+/)
      @traceFn(varName)

    subs.add atom.commands.add 'atom-text-editor', 'clojure-plus:run-and-trace', =>
      @reset()
      editor = atom.workspace.getActiveTextEditor()
      range = protoRepl.EditorUtils.getCursorInBlockRange(editor, topLevel: true)
      protoRepl.clearRepl() if atom.config.get('clojure-plus.clearRepl')
      console.log editor.getTextInRange(range)
      @repl.syncRun(editor.getTextInRange(range)).then (res) =>
        console.log("RES", res)
        @showTrace(editor, range)

    @prepare(subs)

  prepare: (subs) ->
    code = @getFile("~/.atom/packages/clojure-plus/lib/clj/__tracing__.clj")
    [window.code, window.repl] = [code, @repl]
    @repl.lastCmd.then (e) => console.log "REPL READY", e
    @repl.runCodeInNS(code, 'clj.--tracing--').then (e) =>
      console.log("SYNC", e)

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
    values.forEach ({fn, args, children, id, mapping, returned}) =>
      console.log 'DEBUG', [fn, args, children, id, mapping, returned]
      # SETUP
      fatherElement = document.createElement('div')
      functionElement = document.createElement('div')
      childrenElement = document.createElement('div')
      innerTrace = document.createElement('div')
      fatherElement.appendChild(functionElement)
      fatherElement.appendChild(childrenElement)

      # Function PART
      functionSpan = document.createElement('span')
      functionSpan.innerText = "#{fn} => "

      resultDiv = document.createElement('div')
      resultDiv.classList.add('result')
      retHTML = @resToHTMLTree(protoRepl.ednToDisplayTree(returned))
      retHTML.classList.add('result')
      resultDiv.appendChild(retHTML)
      functionElement.appendChild(functionSpan)
      functionElement.appendChild(resultDiv)

      # Children PART
      childElements = []
      if children
        childElements.push @createElements(children, document.createElement('div'))

      # Inner trace PART
      a = document.createElement('a')
      a.innerText = "TRACE"
      a.onclick = =>
        @repl.syncRun("(clj.__tracing__/trace-inner :#{id})").then (res) =>
          console.log "TRACE RES", trace, res
          innerTrace.innerHTML = "Child"
          return unless res.value
          value = protoRepl.parseEdn(res.value)
          return unless value
          @createElements(value, innerTrace)
      innerTrace.appendChild a
      childElements.push innerTrace

      treeHTML = protoRepl.ink.tree.treeView(childrenElement, childElements, {})
      fatherElement.appendChild treeHTML
      d.appendChild(fatherElement)

      # returnedHTML.appendChild(span)
      # returnedHTML.appendChild(retHTML.cloneNode())
      # fn = "(#{fn} #{args.join(" ")})" if !fn.startsWith('(') && args?
      # fnHTML = document.createElement('strong')
      # returned = protoRepl.ednToDisplayTree(returned)
      # retHTML = @resToHTMLTree(returned)
      # retHTML.classList.add('result')
      # span = document.createElement('span')
      # span.innerText = "#{fn} => "
      # fnHTML.appendChild(span)
      # fnHTML.appendChild(retHTML)
      #
      # mappingHTML = document.createElement('div')
      # for k, v of mapping
      #   p = document.createElement('div')
      #   p.innerText = "#{k} => #{v}"
      #   mappingHTML.appendChild(p)
      #
      # returnedHTML = document.createElement('div')
      # span = document.createElement('span')
      # span.innerText = "RETURNED => "
      # returnedHTML.appendChild(span)
      # returnedHTML.appendChild(retHTML.cloneNode())
      #
      # childrenHTML = @createElements(children, document.createElement('div')) if children
      # if !childrenHTML?
      #   a = document.createElement('a')
      #   a.innerText = "TRACE"
      #   a.onclick = =>
      #     @repl.syncRun("(clj.__tracing__/trace-inner :#{id})").then (res) =>
      #       console.log "TRACE RES", trace, res
      #       childrenHTML.innerHTML = ""
      #       return unless res.value
      #       value = protoRepl.parseEdn(res.value)
      #       return unless value
      #       @createElements(value, childrenHTML)
      #   childrenHTML = document.createElement('div')
      #   childrenHTML.appendChild(a)
      #
      # child = [mappingHTML, childrenHTML, returnedHTML]
      # treeHtml = protoRepl.ink.tree.treeView(fnHTML, child, {})
      # d.appendChild(treeHtml)

    if result.setContent
      result.setContent(d)
    else
      result.appendChild(d)

  resToHTMLTree: (returned) ->
    children = returned.slice(2)
    console.log "C", children
    childrenHTML = children.map (c) => @resToHTMLTree(c)
    protoRepl.ink.tree.treeView(returned[0], childrenHTML, {})

  getFile: (file) ->
    home = process.env.HOME
    fileName = file.replace("~", home)
    fs.readFileSync(fileName).toString()
