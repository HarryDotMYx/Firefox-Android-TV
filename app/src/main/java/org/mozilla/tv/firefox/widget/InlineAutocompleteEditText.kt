/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.widget

import android.content.Context
import android.graphics.Rect
import android.text.Editable
import android.text.NoCopySpan
import android.text.Selection
import android.text.Spanned
import android.text.TextUtils
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.utils.UrlUtils
import org.mozilla.tv.firefox.utils.ViewUtils

class InlineAutocompleteEditText(
    context: Context,
    attrs: AttributeSet?
) : AppCompatEditText(context, attrs) {

    fun interface OnCommitListener {
        fun onCommit()
    }

    fun interface OnFilterListener {
        fun onFilter(searchText: String, view: InlineAutocompleteEditText?)
    }

    /**
     * Called when the user inputs some value. This is distinguished from commit (user completed
     * input) and filter (text changed in the view, even by `setText` methods).
     */
    fun interface OnUserInputListener {
        fun onUserInput()
    }

    fun interface OnBackPressedListener {
        fun onBackPressed()
    }

    class AutocompleteResult(
        val text: String,
        val source: String,
        val totalItems: Int
    ) {
        val isEmpty: Boolean
            get() = text.isEmpty()

        val length: Int
            get() = text.length

        fun startsWith(prefix: String): Boolean = text.startsWith(prefix)

        companion object {
            @JvmStatic
            fun emptyResult(): AutocompleteResult = AutocompleteResult("", "", 0)
        }
    }

    private var commitListener: OnCommitListener? = null
    private var filterListener: OnFilterListener? = null
    private var userInputListener: OnUserInputListener? = null
    private var onBackPressedListener: OnBackPressedListener? = null

    // The previous autocomplete result returned to us
    private var autoCompleteResult = AutocompleteResult.emptyResult()
    // If text change is due to us setting autocomplete
    private var settingAutoComplete = false
    // Spans used for marking the autocomplete text
    private var autoCompleteSpans: Array<Any> = emptyArray()
    // Do not process autocomplete result
    private var discardAutoCompleteResult = false

    /**
     * True if the current key press is text entry from the Fire TV Remote app (from Google Play) or
     * a "Clear" press on the soft keyboard, false otherwise. We include the latter due to
     * implementation necessity.
     */
    private var isKeyFromRemoteAppOrSoftKeyboardClear = false

    fun setOnCommitListener(listener: OnCommitListener?) {
        commitListener = listener
    }

    fun setOnFilterListener(listener: OnFilterListener?) {
        filterListener = listener
    }

    fun setOnUserInputListener(listener: OnUserInputListener?) {
        userInputListener = listener
    }

    fun setOnBackPressedListener(listener: OnBackPressedListener?) {
        onBackPressedListener = listener
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setOnKeyListener(KeyListener())
        addTextChangedListener(TextChangeListener())
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)

        if (gainFocus) {
            resetAutocompleteState()
            return
        }

        removeAutocomplete(text!!)

        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        try {
            imm.restartInput(this)
            imm.hideSoftInputFromWindow(windowToken, 0)
        } catch (e: NullPointerException) {
            Log.e(
                LOGTAG,
                "InputMethodManagerService, why are you throwing a NullPointerException? See bug 782096",
                e
            )
        }
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text, type)

        // Any autocomplete text would have been overwritten, so reset our autocomplete states.
        resetAutocompleteState()
    }

    override fun sendAccessibilityEventUnchecked(event: AccessibilityEvent) {
        // We need to bypass the isShown() check in the default implementation
        // for TYPE_VIEW_TEXT_SELECTION_CHANGED events so that accessibility
        // services could detect a url change.
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED &&
            parent != null && !isShown
        ) {
            onInitializeAccessibilityEvent(event)
            dispatchPopulateAccessibilityEvent(event)
            parent.requestSendAccessibilityEvent(this, event)
        } else {
            super.sendAccessibilityEventUnchecked(event)
        }
    }

    /**
     * Mark the start of autocomplete changes so our text change
     * listener does not react to changes in autocomplete text
     */
    private fun beginSettingAutocomplete() {
        beginBatchEdit()
        settingAutoComplete = true
    }

    /**
     * Mark the end of autocomplete changes
     */
    private fun endSettingAutocomplete() {
        settingAutoComplete = false
        endBatchEdit()
    }

    /**
     * Reset autocomplete states to their initial values
     */
    private fun resetAutocompleteState() {
        autoCompleteSpans = arrayOf(
            // Span to mark the autocomplete text
            AUTOCOMPLETE_SPAN,
            // Span to change the autocomplete text color
            BackgroundColorSpan(ContextCompat.getColor(context, R.color.colorAccent))
        )

        autoCompleteResult = AutocompleteResult.emptyResult()
    }

    /**
     * Remove any autocomplete text
     *
     * @param text Current text content that may include autocomplete text
     */
    private fun removeAutocomplete(text: Editable): Boolean {
        val start = text.getSpanStart(AUTOCOMPLETE_SPAN)
        if (start < 0) {
            // No autocomplete text
            return false
        }

        beginSettingAutocomplete()

        // When we call delete() here, the autocomplete spans we set are removed as well.
        text.delete(start, text.length)

        endSettingAutocomplete()
        return true
    }

    /**
     * Convert any autocomplete text to regular text
     *
     * @param text Current text content that may include autocomplete text
     */
    private fun commitAutocomplete(text: Editable): Boolean {
        val start = text.getSpanStart(AUTOCOMPLETE_SPAN)
        if (start < 0) {
            // No autocomplete text
            return false
        }

        beginSettingAutocomplete()

        // Remove all spans here to convert from autocomplete text to regular text
        for (span in autoCompleteSpans) {
            text.removeSpan(span)
        }

        endSettingAutocomplete()

        // Filter on the new text
        filterListener?.onFilter(text.toString(), null)
        return true
    }

    /**
     * Add autocomplete text based on the result URI.
     *
     * @param result Result URI to be turned into autocomplete text
     */
    fun onAutocomplete(result: AutocompleteResult?) {
        // If discardAutoCompleteResult is true, we temporarily disabled
        // autocomplete (due to backspacing, etc.) and we should bail early.
        if (discardAutoCompleteResult || isKeyFromRemoteAppOrSoftKeyboardClear) {
            return
        }

        if (!isEnabled || result == null || result.isEmpty) {
            autoCompleteResult = AutocompleteResult.emptyResult()
            return
        }

        val editable = text!!
        val textLength = editable.length
        val resultLength = result.length
        val autoCompleteStart = editable.getSpanStart(AUTOCOMPLETE_SPAN)
        autoCompleteResult = result

        if (autoCompleteStart > -1) {
            // Autocomplete text already exists; we should replace existing autocomplete text.

            // If the result and the current text don't have the same prefixes,
            // the result is stale and we should wait for the another result to come in.
            if (!TextUtils.regionMatches(result.text, 0, editable, 0, autoCompleteStart)) {
                return
            }

            beginSettingAutocomplete()

            // Replace the existing autocomplete text with new one.
            // replace() preserves the autocomplete spans that we set before.
            editable.replace(autoCompleteStart, textLength, result.text, autoCompleteStart, resultLength)

            endSettingAutocomplete()
        } else {
            // No autocomplete text yet; we should add autocomplete text

            // If the result prefix doesn't match the current text,
            // the result is stale and we should wait for the another result to come in.
            if (resultLength <= textLength ||
                !TextUtils.regionMatches(result.text, 0, editable, 0, textLength)
            ) {
                return
            }

            val spans = editable.getSpans(textLength, textLength, Any::class.java)
            val spanStarts = IntArray(spans.size)
            val spanEnds = IntArray(spans.size)
            val spanFlags = IntArray(spans.size)

            // Save selection/composing span bounds so we can restore them later.
            for (i in spans.indices) {
                val span = spans[i]
                val spanFlag = editable.getSpanFlags(span)

                // We don't care about spans that are not selection or composing spans.
                // For those spans, spanFlag[i] will be 0 and we don't restore them.
                if ((spanFlag and Spanned.SPAN_COMPOSING) == 0 &&
                    span !== Selection.SELECTION_START &&
                    span !== Selection.SELECTION_END
                ) {
                    continue
                }

                spanStarts[i] = editable.getSpanStart(span)
                spanEnds[i] = editable.getSpanEnd(span)
                spanFlags[i] = spanFlag
            }

            beginSettingAutocomplete()

            // First add trailing text.
            editable.append(result.text, textLength, resultLength)

            // Restore selection/composing spans.
            for (i in spans.indices) {
                val spanFlag = spanFlags[i]
                if (spanFlag == 0) {
                    // Skip if the span was ignored before.
                    continue
                }
                editable.setSpan(spans[i], spanStarts[i], spanEnds[i], spanFlag)
            }

            // Mark added text as autocomplete text.
            for (span in autoCompleteSpans) {
                editable.setSpan(span, textLength, resultLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            // Make sure the autocomplete text is visible.
            bringPointIntoView(resultLength)

            endSettingAutocomplete()
        }

        announceForAccessibility(editable.toString())
    }

    val lastAutocompleteResult: AutocompleteResult
        get() = autoCompleteResult

    private inner class TextChangeListener : TextWatcher {
        private var textLengthBeforeChange = 0

        override fun afterTextChanged(editable: Editable) {
            if (!isEnabled || settingAutoComplete) {
                return
            }

            val text = getNonAutocompleteText(editable)
            val textLength = text.length
            var doAutocomplete = true

            if (UrlUtils.isSearchQuery(text) ||
                isKeyFromRemoteAppOrSoftKeyboardClear // See var use in onAutocomplete.
            ) {
                doAutocomplete = false
            } else if (textLength == textLengthBeforeChange - 1 || textLength == 0) {
                // If you're hitting backspace (the string is getting smaller), don't autocomplete
                doAutocomplete = false
            }

            // If we are not autocompleting, we set discardAutoCompleteResult to true
            // to discard any autocomplete results that are in-flight, and vice versa.
            discardAutoCompleteResult = !doAutocomplete

            if (doAutocomplete && autoCompleteResult.startsWith(text)) {
                // If this text already matches our autocomplete text, autocomplete likely
                // won't change. Just reuse the old autocomplete value.
                onAutocomplete(autoCompleteResult)
                doAutocomplete = false
            } else {
                // Otherwise, remove the old autocomplete text
                // until any new autocomplete text gets added.
                removeAutocomplete(editable)
            }

            filterListener?.onFilter(text, if (doAutocomplete) this@InlineAutocompleteEditText else null)
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            textLengthBeforeChange = s.length
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            // do nothing
        }
    }

    private inner class KeyListener : View.OnKeyListener {
        override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                if (event.action != KeyEvent.ACTION_DOWN) {
                    return true
                }

                commitListener?.onCommit()

                return true
            }

            if ((keyCode == KeyEvent.KEYCODE_DEL || keyCode == KeyEvent.KEYCODE_FORWARD_DEL) &&
                removeAutocomplete(text!!)
            ) {
                // Delete autocomplete text when backspacing or forward deleting.
                return true
            }

            return false
        }
    }

    /**
     * Code to handle deleting autocomplete first when backspacing.
     * If there is no autocomplete text, both removeAutocomplete() and commitAutocomplete()
     * are no-ops and return false. Therefore we can use them here without checking explicitly
     * if we have autocomplete text or not.
     */
    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val ic = super.onCreateInputConnection(outAttrs) ?: return null

        return object : InputConnectionWrapper(ic, false) {
            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                removeAutocomplete(text!!)
                return super.deleteSurroundingText(beforeLength, afterLength)
            }

            private fun removeAutocompleteOnComposing(text: CharSequence): Boolean {
                val editable = this@InlineAutocompleteEditText.text!!
                val composingStart = BaseInputConnection.getComposingSpanStart(editable)
                val composingEnd = BaseInputConnection.getComposingSpanEnd(editable)
                // We only delete the autocomplete text when the user is backspacing,
                // i.e. when the composing text is getting shorter.
                if (composingStart >= 0 &&
                    composingEnd >= 0 &&
                    (composingEnd - composingStart) > text.length &&
                    removeAutocomplete(editable)
                ) {
                    // Make the IME aware that we interrupted the setComposingText call,
                    // by having finishComposingText() send change notifications to the IME.
                    finishComposingText()
                    setComposingRegion(composingStart, composingEnd)
                    return true
                }
                return false
            }

            override fun commitText(text: CharSequence, newCursorPosition: Int): Boolean {
                userInputListener?.onUserInput()

                if (isCommitTextFromRemoteAppOrSoftKeyboardClear(text)) {
                    setIsKeyFromRemoteAppOrSoftKeyboardClear(true)
                }

                if (removeAutocompleteOnComposing(text)) {
                    return false
                }
                return super.commitText(text, newCursorPosition)
            }

            private fun isCommitTextFromRemoteAppOrSoftKeyboardClear(text: CharSequence): Boolean {
                // Two events call this with text as the empty string: Clear from the soft keyboard
                // and input from the remote app (which calls this with the empty string to
                // clear the input, then calls this with the full input).
                return "" == text
            }

            override fun setComposingText(text: CharSequence, newCursorPosition: Int): Boolean {
                if (removeAutocompleteOnComposing(text)) {
                    return false
                }
                return super.setComposingText(text, newCursorPosition)
            }
        }
    }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        // The remote app doesn't fire this key event when entering characters into the url bar so
        // it must not be the remote app. Note that the remote app does fire this for focusing the
        // url bar though.
        setIsKeyFromRemoteAppOrSoftKeyboardClear(false)

        if (isAttachedToWindow) {
            // We only want to process one event per tap
            if (event.action != KeyEvent.ACTION_DOWN) {
                return false
            }

            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                // If the edit text has a composition string, don't submit the text yet.
                // ENTER is needed to commit the composition string.
                val content = text!!
                if (!hasCompositionString(content)) {
                    commitListener?.onCommit()

                    return true
                }
            }

            if (keyCode == KeyEvent.KEYCODE_BACK) {
                removeAutocomplete(text!!)
                onBackPressedListener?.onBackPressed()
                // Issue #495 - Handle hiding keyboard so Amazon keyboard doesn't mess up the focus.
                return ViewUtils.hideKeyboard(this)
            }

            return false
        }

        return false
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        if (isAttachedToWindow) {
            // The user has repositioned the cursor somewhere. We need to adjust
            // the autocomplete text depending on where the new cursor is.

            val editable = text!!
            val start = editable.getSpanStart(AUTOCOMPLETE_SPAN)

            if (settingAutoComplete || start < 0 || (start == selStart && start == selEnd)) {
                // Do not commit autocomplete text if there is no autocomplete text
                // or if selection is still at start of autocomplete text
                return
            }

            if (selStart <= start && selEnd <= start) {
                // The cursor is in user-typed text; remove any autocomplete text.
                removeAutocomplete(editable)
            } else {
                // The cursor is in the autocomplete text; commit it so it becomes regular text.
                commitAutocomplete(editable)
            }
        }

        super.onSelectionChanged(selStart, selEnd)
    }

    private fun setIsKeyFromRemoteAppOrSoftKeyboardClear(isKeyFromRemoteApp: Boolean) {
        isKeyFromRemoteAppOrSoftKeyboardClear = isKeyFromRemoteApp
        if (isKeyFromRemoteApp) {
            resetAutocompleteState() // We want a blank autocomplete result for telemetry.
            removeAutocomplete(text!!) // Perhaps not strictly necessary.
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // This prevents the selector from entering the text area
        val keyCode = event.keyCode
        val shouldDispatch = !(
            keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                keyCode == KeyEvent.KEYCODE_DPAD_UP
            )

        return shouldDispatch && super.dispatchKeyEvent(event)
    }

    companion object {
        private const val LOGTAG = "GeckoToolbarEditText"
        private val AUTOCOMPLETE_SPAN: NoCopySpan = NoCopySpan.Concrete()

        /**
         * Get the portion of text that is not marked as autocomplete text.
         *
         * @param text Current text content that may include autocomplete text
         */
        private fun getNonAutocompleteText(text: Editable): String {
            val start = text.getSpanStart(AUTOCOMPLETE_SPAN)
            if (start < 0) {
                // No autocomplete text; return the whole string.
                return text.toString()
            }

            // Only return the portion that's not autocomplete text
            return TextUtils.substring(text, 0, start)
        }

        private fun hasCompositionString(content: Editable): Boolean {
            val spans = content.getSpans(0, content.length, Any::class.java)

            for (span in spans) {
                if ((content.getSpanFlags(span) and Spanned.SPAN_COMPOSING) != 0) {
                    // Found composition string.
                    return true
                }
            }

            return false
        }
    }
}
