package com.imkiva.xart.editor.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.OverScroller;

import com.imkiva.xart.ServiceManager;
import com.imkiva.xart.editor.common.Document;
import com.imkiva.xart.event.InternalErrorEvent;
import com.imkiva.xart.eventbus.api.IEventManager;
import com.imkiva.xart.language.Language;
import com.imkiva.xart.language.ParserDefinition;
import com.imkiva.xart.editor.api.lexer.HighlightSpan;
import com.imkiva.xart.editor.api.lexer.HighlightTokenType;
import com.imkiva.xart.editor.api.listener.OnAutoCompletionListener;
import com.imkiva.xart.editor.api.listener.OnEditActionListener;
import com.imkiva.xart.editor.api.skin.LightSkin;
import com.imkiva.xart.editor.api.skin.Skin;
import com.imkiva.xart.editor.common.DocumentProvider;
import com.imkiva.xart.editor.highlight.Lexer;
import com.imkiva.xart.editor.model.Pair;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

public class XartEditor extends View {

    /**
     * 光标跳动的时间
     */
    private static final long CURSOR_JUMP_TIME = 600L;

    /**
     * 空字符宽度相对于正常字符宽度的比例
     */
    protected static final float EMPTY_CURSOR_WIDTH_SCALE = 0.75f;

    /**
     * 默认的Tab所占空格数
     */
    protected static final int DEFAULT_TAB_LENGTH_SPACES = 4;

    /**
     * 可以滑动的X轴空白范围
     */
    private static final float FREE_SCROLL_SPACE_X = 1000;

    /**
     * 可以滑动的Y轴空白返回
     */
    private static final float FREE_SCROLL_SPACE_Y = 1000;

    protected static final int BASE_TEXT_SIZE_PIXELS = 16;

    /**
     * 滚动控制器
     */
    private final OverScroller mScroller;

    /**
     * 绘制文本使用的画笔
     */
    private Paint mTextPaint;

    /**
     * 行号所占边距
     */
    protected int mLeftPadding;

    /**
     * 触摸行为
     */
    protected TouchNavigationMethod mTouchNavigationMethod;

    /**
     * 提供文本内容
     */
    protected DocumentProvider mDocument;

    /**
     * 当前光标的位置
     */
    protected int mCursorPosition = 0;

    /**
     * 当前选择高亮的左侧偏移
     */
    protected int mSelectionLeft = -1;

    /**
     * 当前选择高亮的右侧偏移
     */
    protected int mSelectionRight = -1;

    /**
     * Tab所占空格数
     */
    protected int mTabLength = DEFAULT_TAB_LENGTH_SPACES;

    /**
     * Tab所占空格对应的文本
     */
    protected String mTabSpaceContent = makeTabs();

    /**
     * 主题
     */
    protected Skin mSkin = LightSkin.getInstance();

    /**
     * 是否高亮当前所在行
     */
    protected boolean isHighlightCurrentLine = true;

    /**
     * 是否开启自动缩进
     */
    protected boolean isAutoIndent = true;

    /**
     * 编辑行为控制器
     */
    private EditBehaviorController mEditBehaviorController;

    /**
     * 与输入法的连接
     */
    private TextFieldInputConnection mInputConnection;

    /**
     * 光标所在行改变监听器
     */
    private LineChangeListener mLineChangeListener;

    /**
     * 光标所在行改变监听器
     */
    private SelectionModeChangeListener mSelectionModeChangeListener;

    /**
     * 光标所在行
     */
    private int mCursorLine = 0;

    /**
     * 最长的一行的宽度
     */
    private int mMaxTextWidth = 0;

    /**
     * 缓存advances,这样就不用每次都计算
     */
    private int[] mAdvances;

    /**
     * 是否可以编辑
     */
    private boolean isEditable = true;

    /**
     * 用于控制光标显示和消失的变量
     */
    private boolean mShowCursor;

    /**
     * 一个控制光标显示和消失的线程
     */
    private CursorLooperThread mCursorThread;

    /**
     *
     */
    private OnEditActionListener mOnEditActionListener;

    private Language mLanguage = new Language();

    private boolean mCursorVisible = true;

    public XartEditor(Context context) {
        this(context, null);
    }

    public XartEditor(Context context, AttributeSet attrs) {
        super(context, attrs);

        mScroller = new OverScroller(context);
        commonInit();
        initView();
    }


    public XartEditor(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mScroller = new OverScroller(context);
        commonInit();
        initView();
    }

    public float getTextSize() {
        return mTextPaint.getTextSize();
    }


    public void setTextSize(float textSize) {
        this.mTextPaint.setTextSize(textSize);
        updateLeftPadding();
        mEditBehaviorController.updateCursorLine();
        if (!makeCharVisible(mCursorPosition)) {
            invalidate();
        }
    }

    public void release() {
        if (mCursorThread != null) {
            mCursorThread.interrupt();
            mCursorThread = null;
        }

    }

    protected void commonInit() {
        //默认的监听器
        mLineChangeListener = newLineIndex -> {
        };

        mSelectionModeChangeListener = active -> {
        };

        mDocument = new DocumentProvider();
        mTouchNavigationMethod = new TouchNavigationMethod(this);
    }
    protected void initView() {
        this.mAdvances = new int[128];
        Arrays.fill(this.mAdvances, -1);
        mEditBehaviorController = this.new EditBehaviorController();

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(BASE_TEXT_SIZE_PIXELS);

        setBackgroundColor(mSkin.getColor(Skin.Colorable.BACKGROUND));
        setLongClickable(false);
        setFocusableInTouchMode(true);
        setHapticFeedbackEnabled(true);

        resetView();
        setScrollContainer(true);

        mCursorThread = new CursorLooperThread(new WeakReference<XartEditor>(this));
        mCursorThread.start();
    }

    public TouchNavigationMethod getTouchMethod() {
        return mTouchNavigationMethod;
    }

    public boolean canUndo() {
        return mDocument.canUndo();
    }

    public boolean canRedo() {
        return mDocument.canRedo();
    }

    /**
     * 后退
     *
     * @return 数量
     */
    public int undo() {
        int result = mDocument.undo();

        invalidate();
        refreshSpans();
        updateLeftPadding();

        return result;
    }

    /**
     * 重做
     *
     * @return 数量
     */
    public int redo() {
        int result = mDocument.redo();

        invalidate();
        refreshSpans();
        updateLeftPadding();

        return result;
    }

    /**
     * 重置视图
     */
    private void resetView() {
        mCursorPosition = 0;
        mCursorLine = 0;
        mMaxTextWidth = 0;
        mEditBehaviorController.setSelectText(false);
        mEditBehaviorController.stopTextComposing();
        mDocument.clearSpans();
        mLineChangeListener.onLineChange(0);
        scrollTo(0, 0);
    }

    /**
     * 设置新的内容提供器
     */
    public void setDocumentProvider(DocumentProvider hDoc) {
        mDocument = hDoc;
        mDocument.setOnEditActionListener(mOnEditActionListener);
        resetView();
        mEditBehaviorController.cancelSpanning(); //stop existing lex threads
        mEditBehaviorController.refreshSpans();
        invalidate();
    }


    /**
     * @return 当前内容提供器的副本
     */
    public DocumentProvider cloneDocumentProvider() {
        return new DocumentProvider(mDocument);
    }

    /**
     * @return 文本长度
     */
    public int length() {
        return mDocument.docLength();
    }

    /**
     * 设置行改变监听器
     *
     * @param lineChangeListener 行改变监听器
     */
    public void setLineChangeListener(LineChangeListener lineChangeListener) {
        this.mLineChangeListener = lineChangeListener;
    }

    /**
     * 设置选择模式改变监听器
     *
     * @param selectionModeChangeListener 选择模式改变监听器
     */
    public void setSelModeListener(SelectionModeChangeListener selectionModeChangeListener) {
        this.mSelectionModeChangeListener = selectionModeChangeListener;
    }

    /**
     *
     */
    public void setNavigationMethod(TouchNavigationMethod navMethod) {
        mTouchNavigationMethod = navMethod;
    }

    /**
     * 在指定位置插入一段文本
     *
     * @param pos  位置
     * @param text 要插入的文本
     */
    public void insert(int pos, String text) {
        mDocument.insertBefore(text.toCharArray(), pos, System.nanoTime());
        updateLeftPadding();
        refreshSpans();
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_ENTER_ACTION
                | EditorInfo.IME_ACTION_DONE
                | EditorInfo.IME_FLAG_NO_EXTRACT_UI;
        if (mInputConnection == null) {
            mInputConnection = this.new TextFieldInputConnection(this);
        } else {
            mInputConnection.resetComposingState();
        }
        return mInputConnection;
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }

    @Override
    public boolean isSaveEnabled() {
        return true;
    }

    //---------------------------------------------------------------------
    //------------------------- Layout 方法 ----------------------------
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(useAllDimensions(widthMeasureSpec),
                useAllDimensions(heightMeasureSpec));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mEditBehaviorController.updateCursorLine();
        if (!makeCharVisible(mCursorPosition)) {
            invalidate();
        }
    }

    private int useAllDimensions(int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int result = MeasureSpec.getSize(measureSpec);

        if (specMode != MeasureSpec.EXACTLY && specMode != MeasureSpec.AT_MOST) {
            result = Integer.MAX_VALUE;
        }

        return result;
    }

    /**
     * @return 屏幕可见的行数
     */
    public int getVisibleLines() {
        return (int) Math.ceil((double) getContentHeight() / lineHeight());
    }

    /**
     * @return 一行的高度
     */
    public int lineHeight() {
        Paint.FontMetricsInt metrics = mTextPaint.getFontMetricsInt();
        return (metrics.descent - metrics.ascent);
    }

    /**
     * @return 内容高度
     */
    protected int getContentHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    /**
     * @return 内容宽度
     */
    protected int getContentWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    /**
     * @return 左边距
     */
    @Override
    public int getPaddingLeft() {
        return super.getPaddingLeft() + this.mLeftPadding;
    }

    /**
     * @return 行号宽度
     */
    public int getLineNumberPadding() {
        return mLeftPadding;
    }

    /**
     * @return 可见的第一行
     */
    private int getBeginPaintLine(Canvas canvas) {
        Rect bounds = canvas.getClipBounds();
        if (bounds.top == 0) {
            bounds.top = 1;
        }
        return bounds.top / lineHeight();
    }

    /**
     * @return 可见的最后一行
     */
    private int getEndPaintLine(Canvas canvas) {
        Rect bounds = canvas.getClipBounds();
        return (bounds.bottom - 1) / lineHeight();
    }

    /**
     * @return 给定行的行基址
     */
    private int getPaintBaseline(int line) {
        Paint.FontMetricsInt metrics = mTextPaint.getFontMetricsInt();
        return (line + 1) * lineHeight() - metrics.descent;
    }

    /**
     * 完整绘制流程
     *
     * @param canvas 画布
     */
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.clipRect(getScrollX() + super.getPaddingLeft(),
                getScrollY() + getPaddingTop(),
                (getScrollX() + getWidth()) - getPaddingRight()
                , (getScrollY() + getHeight()) - getPaddingBottom());
        canvas.translate(super.getPaddingLeft(), getPaddingTop());
        drawContent(canvas);
        canvas.restore();
        this.mTouchNavigationMethod.onTextDrawComplete(canvas);
    }


    public DocumentProvider getDoc() {
        return mDocument;
    }

    private void drawContent(Canvas canvas) {
        //可见第一行
        int beginPaintLine = getBeginPaintLine(canvas);

        //可见最后一行
        int endPaintLine = getEndPaintLine(canvas);

        //第一行的baseline
        int paintBaseline = getPaintBaseline(beginPaintLine);

        //已经绘制到的偏移量
        int currentOffset = this.mDocument.getLineOffset(beginPaintLine);

        if (currentOffset < 0) {
            return;
        }
        //如果开启了高亮当前所在行 并且光标在可见区域内, 画当前所在行的Rect
        if (this.isHighlightCurrentLine && beginPaintLine <= mCursorLine && endPaintLine >= mCursorLine) {
            this.mTextPaint.setColor(this.mSkin.getColor(Skin.Colorable.LINE_HIGHLIGHT));
            drawTextBackground(canvas, this.mLeftPadding, getPaintBaseline(mCursorLine), getWidth());
        }

        //无论什么时候,List中至少有一个Span
        List<HighlightSpan> highlightSpans = this.mDocument.getSpans();

        HighlightSpan prevHighlightSpan;
        HighlightSpan currHighlightSpan = highlightSpans.get(0);
        int currIndex;
        int nextIndex = 1;

        do {
            prevHighlightSpan = currHighlightSpan;
            if (nextIndex < highlightSpans.size()) {
                currIndex = nextIndex;
                currHighlightSpan = highlightSpans.get(currIndex);
                nextIndex++;
            } else {
                currHighlightSpan = null;
            }
            if (currHighlightSpan == null) {
                break;
            }
        } while (currHighlightSpan.getOffset() <= currentOffset);

        HighlightTokenType highlightTokenType = prevHighlightSpan.getHighlightTokenType();
        int tokenColor = this.mSkin.getTokenColor(prevHighlightSpan);
        this.mTextPaint.setColor(tokenColor);
        int currentSpanOffset = currHighlightSpan != null ? currHighlightSpan.getOffset() : -1;

        //当前所在行的行号
        int currentLineNumber = this.mDocument.findLineNumber(currentOffset);

        while (beginPaintLine <= endPaintLine) {
            String replace = this.mDocument.getLine(beginPaintLine).replace(ParserDefinition.EOF, '\u0000');
            if (replace.length() == 0) {
                break;
            }
            currentLineNumber++;

            //如果行号看不见,就不用画了
            if (this.mLeftPadding > getScrollX()) {
                this.mTextPaint.setColor(Color.GRAY);
                canvas.drawText(String.valueOf(currentLineNumber), 0.0f, paintBaseline, this.mTextPaint);
                this.mTextPaint.setColor(tokenColor);
            }

            int lineExtend = this.mLeftPadding;


            int cur = 0;
            while (cur < replace.length()) {
                if (currentOffset == currentSpanOffset) {
                    highlightTokenType = currHighlightSpan != null ? currHighlightSpan.getHighlightTokenType() : HighlightTokenType.NORMAL;
                    tokenColor = this.mSkin.getTokenColor(currHighlightSpan);
                    this.mTextPaint.setColor(tokenColor);
                    if (nextIndex < highlightSpans.size()) {
                        currIndex = nextIndex;
                        nextIndex++;
                        currHighlightSpan = highlightSpans.get(currIndex);
                        currentSpanOffset = currHighlightSpan.getOffset();
                    } else {
                        currHighlightSpan = null;
                        currentSpanOffset = -1;
                    }
                }
                if (currentOffset == this.mCursorPosition) {
                    if (isFocused()) {
                        this.mTextPaint.setColor(this.mSkin.getColor(Skin.Colorable.CURSOR_BACKGROUND));
                    } else {
                        this.mTextPaint.setColor(this.mSkin.getColor(Skin.Colorable.CURSOR_DISABLED));
                    }
                    if (this.mShowCursor) {
                        drawTextBackground(canvas, lineExtend, paintBaseline, 2);
                    }
                    this.mTextPaint.setColor(tokenColor);
                }
                int min;
                if (inSelectionRange(currentOffset)) {

                    min = Math.min((getSelectionEnd() - currentOffset) + cur, replace.length());
                    if (this.mCursorPosition > currentOffset) {
                        min = Math.min(min, (this.mCursorPosition - currentOffset) + cur);
                    }
                    lineExtend += drawSelectedText(canvas,
                            replace.substring(cur, min).replace("\t", mTabSpaceContent),
                            lineExtend,
                            paintBaseline);
                    currentOffset += min - cur;
                    cur = min - 1;

                    while (currentOffset > currentSpanOffset && currentSpanOffset != -1) {
                        if (nextIndex < highlightSpans.size()) {
                            currIndex = nextIndex;
                            nextIndex++;
                            currHighlightSpan = highlightSpans.get(currIndex);
                            currentSpanOffset = currHighlightSpan.getOffset();
                        } else {
                            currHighlightSpan = null;
                            currentSpanOffset = -1;
                        }
                    }
                } else {
                    min = replace.length();
                    if (currentSpanOffset > currentOffset) {
                        min = Math.min((currentSpanOffset - currentOffset) + cur, min);
                    }
                    if (isSelectText()) {
                        if (getSelectionStart() > currentOffset) {
                            min = Math.min(min, (getSelectionStart() - currentOffset) + cur);
                        }
                    } else if (this.mCursorPosition > currentOffset) {
                        min = Math.min(min, (this.mCursorPosition - currentOffset) + cur);
                    }
                    String replaceIndent = replace.substring(cur, min).replace("\t", mTabSpaceContent);

                    lineExtend += drawString(canvas, replaceIndent, lineExtend, paintBaseline, highlightTokenType == HighlightTokenType.KEYWORD);
                    currentOffset += min - cur;
                    cur = min - 1;
                }
                cur++;
            }

            //准备画下一行
            paintBaseline += lineHeight();

            if (lineExtend > this.mMaxTextWidth) {
                this.mMaxTextWidth = lineExtend;
            }
            beginPaintLine++;
        }
    }

    class SpanIndexer {
        List<HighlightSpan> highlightSpans;
        ListIterator<HighlightSpan> spanIterator;
        HighlightSpan currentHighlightSpan;
        int position;

        public SpanIndexer(List<HighlightSpan> highlightSpans) {
            this.highlightSpans = highlightSpans;
        }

        public void toPosition(int position) {
            this.position = position;
            spanIterator = highlightSpans.listIterator();
            currentHighlightSpan = spanIterator.next();
            while (spanIterator.hasNext() && currentHighlightSpan.getOffset() < position) {
                currentHighlightSpan = spanIterator.next();
            }
            if (currentHighlightSpan.getOffset() < position) {
                currentHighlightSpan = null;
            }
        }

        public HighlightSpan next() {
            if (spanIterator.hasNext()) {
                currentHighlightSpan = spanIterator.next();
            } else {
                currentHighlightSpan = null;
            }
            return currentHighlightSpan;
        }

        public HighlightSpan prev() {
            if (spanIterator.hasPrevious()) {
                currentHighlightSpan = spanIterator.previous();
            } else {
                currentHighlightSpan = null;
            }
            return currentHighlightSpan;
        }
    }

    /**
     * 创建Tab所占空格的文本
     *
     * @return 空格字符串
     */
    private String makeTabs() {
        StringBuilder stringBuilder = new StringBuilder(mTabLength);
        for (int i = 0; i < mTabLength; i++) {
            stringBuilder.append(" ");
        }
        return stringBuilder.toString();
    }

    /**
     * 绘制选择区域的文本
     *
     * @param canvas 画布
     * @param text   选择范围内的文本
     * @param x      开始X轴
     * @param y      开始Y轴
     * @return 绘制的长度
     */
    private int drawSelectedText(final Canvas canvas, final String text, final int x, final int y) {
        final int color = this.mTextPaint.getColor();
        final int length = (int) this.mTextPaint.measureText(text);
        this.mTextPaint.setColor(this.mSkin.getColor(Skin.Colorable.SELECTION_BACKGROUND));
        this.drawTextBackground(canvas, x, y, length);
        this.mTextPaint.setColor(this.mSkin.getColor(Skin.Colorable.SELECTION_FOREGROUND));
        this.drawString(canvas, text, x, y, false);
        this.mTextPaint.setColor(color);
        return length;
    }

    /**
     * 绘制字符串
     */
    private float drawString(final Canvas canvas, final String s, final float x, final float y, boolean isBold) {
        mTextPaint.setFakeBoldText(isBold);
        canvas.drawText(s, x, y, this.mTextPaint);
        mTextPaint.setFakeBoldText(false);
        return mTextPaint.measureText(s);
    }

    /**
     * 绘制文本背景
     */
    private void drawTextBackground(Canvas canvas, int paintX, int paintY,
                                    int advance) {
        Paint.FontMetricsInt metrics = mTextPaint.getFontMetricsInt();
        canvas.drawRect(paintX,
                paintY + metrics.ascent,
                paintX + advance,
                paintY + metrics.descent,
                mTextPaint);
    }

    protected int getSpaceAdvance() {
        return (int) mTextPaint.measureText(" ", 0, 1);
    }

    protected int getEOLAdvance() {
        return (int) (EMPTY_CURSOR_WIDTH_SCALE * mTextPaint.measureText(" ", 0, 1));
    }

    protected int getTabAdvance() {
        return mTabLength * (int) mTextPaint.measureText(" ", 0, 1);
    }

    /**
     * Invalidate rows from startRow (inclusive) to endRow (exclusive)
     */
    private void invalidateLines(int startLine, int endLine) {

        Rect cursorSpill = mTouchNavigationMethod.getCursorBloat();
        Paint.FontMetricsInt metrics = mTextPaint.getFontMetricsInt();
        int top = startLine * lineHeight() + getPaddingTop();
        top -= Math.max(cursorSpill.top, metrics.descent);
        top = Math.max(0, top);
        super.invalidate(0,
                top,
                getScrollX() + getWidth(),
                endLine * lineHeight() + getPaddingTop() + cursorSpill.bottom);
    }

    /**
     * 重绘指定行
     */
    private void invalidateFromLine(int lineNumber) {
        Rect CursorSpill = mTouchNavigationMethod.getCursorBloat();
        Paint.FontMetricsInt metrics = mTextPaint.getFontMetricsInt();
        int top = lineNumber * lineHeight() + getPaddingTop();
        top -= Math.max(CursorSpill.top, metrics.descent);
        top = Math.max(0, top);
        super.invalidate(0,
                top,
                getScrollX() + getWidth(),
                getScrollY() + getHeight());
    }


    //---------------------------------------------------------------------
    //-------------------滑动与触摸 -----------------------------

    private void invalidateCursorLine() {
        invalidateLines(mCursorLine, mCursorLine + 1);
    }

    private void invalidateSelectionLines() {
        int startLine = mDocument.findLineNumber(mSelectionLeft);
        int endLine = mDocument.findLineNumber(mSelectionRight);

        invalidateLines(startLine, endLine + 1);
    }

    /**
     * Scrolls the text horizontally and/or vertically if the character
     * specified by charOffset is not in the visible text region.
     * The view is invalidated if it is scrolled.
     *
     * @param charOffset The index of the character to make visible
     * @return True if the drawing area was scrolled horizontally
     * and/or vertically
     */
    private boolean makeCharVisible(int charOffset) {

        int scrollVerticalBy = makeCharLineVisible(charOffset);
        int scrollHorizontalBy = makeCharColumnVisible(charOffset);

        if (scrollVerticalBy == 0 && scrollHorizontalBy == 0) {
            return false;
        } else {
            scrollBy(scrollHorizontalBy, scrollVerticalBy);
            return true;
        }
    }

    /**
     * 计算到达指定字符偏移需要滚动的Y轴相对偏移如果指定的字符偏移不在视图可见范围内
     *
     * @param charOffset 字符偏移
     * @return 需要滚动的Y轴相对偏移
     */
    private int makeCharLineVisible(int charOffset) {
        int scrollBy = 0;
        int charTop = mDocument.findLineNumber(charOffset) * lineHeight();
        int charBottom = charTop + lineHeight();

        if (charTop < getScrollY()) {
            scrollBy = charTop - getScrollY();
        } else if (charBottom > (getScrollY() + getContentHeight())) {
            scrollBy = charBottom - getScrollY() - getContentHeight();
        }

        return scrollBy;
    }

    /**
     * 计算到达指定字符偏移需要滚动的X轴相对偏移如果指定的字符偏移不在视图可见范围内
     *
     * @param charOffset 字符偏移
     * @return 需要滚动的X轴相对偏移
     */
    private int makeCharColumnVisible(int charOffset) {
        int scrollBy = 0;
        Pair visibleRange = getCharExtent(charOffset);

        int charLeft = visibleRange.first;
        int charRight = visibleRange.second;

        if (charRight > (getScrollX() + getContentWidth())) {
            scrollBy = charRight - getScrollX() - getContentWidth();
        }

        if (charLeft < getScrollX()) {
            scrollBy = charLeft - getScrollX();
        }

        return scrollBy;
    }

    /**
     * Calculates the x-coordinate extent of charOffset.
     *
     * @return The x-values of left and right edges of charOffset. Pair.first
     * contains the left edge and Pair.second contains the right edge
     */
    protected Pair getCharExtent(int charOffset) {
        int line = mDocument.findLineNumber(charOffset);
        int currOffset = mDocument.seekChar(mDocument.getLineOffset(line));
        int left = 0;
        int right = 0;

        while (currOffset <= charOffset && mDocument.hasNext()) {
            left = right;
            char c = mDocument.next();
            switch (c) {
                case ' ':
                    right += getSpaceAdvance();
                    break;
                case ParserDefinition.NEWLINE:
                case ParserDefinition.EOF:
                    right += getEOLAdvance();
                    break;
                case ParserDefinition.TAB:
                    right += getTabAdvance();
                    break;
                default:
                    char[] ca = {c};
                    right += (int) mTextPaint.measureText(ca, 0, 1);
                    break;
            }
            ++currOffset;
        }

        return new Pair(left, right);
    }

    /**
     * @param charOffset 字符串偏移量(位置)
     * @return 包含指定字符串偏移的Rect
     */
    Rect getBoundingBox(int charOffset) {
        if (charOffset < 0 || charOffset >= mDocument.docLength()) {
            return new Rect(-1, -1, -1, -1);
        }

        int line = mDocument.findLineNumber(charOffset);
        int top = line * lineHeight();
        int bottom = top + lineHeight();

        Pair xExtent = getCharExtent(charOffset);
        int left = xExtent.first;
        int right = xExtent.second;

        return new Rect(left, top, right, bottom);
    }

    public Skin getSkin() {
        return mSkin;
    }

    public void setSkin(Skin skin) {
        this.mSkin = skin;
        mTouchNavigationMethod.onColorSchemeChanged(skin);
        setBackgroundColor(skin.getColor(Skin.Colorable.BACKGROUND));
    }


    public int getFontHeight() {
        Paint.FontMetricsInt fontMetricsInt = this.mTextPaint.getFontMetricsInt();
        return fontMetricsInt.descent - fontMetricsInt.ascent;
    }

    /**
     * Maps a coordinate to the character that it is on. If the coordinate is
     * on empty space, the nearest character on the corresponding row is returned.
     * If there is no character on the row, -1 is returned.
     * <p/>
     * The coordinates passed in should not have padding applied to them.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @return The index of the closest character, or -1 if there is
     * no character or nearest character at that coordinate
     */
    public int coordinateToCharIndex(int x, int y) {
        int line = y / lineHeight();
        int charIndex = mDocument.getLineOffset(line);

        if (charIndex < 0) {
            //non-existent row
            return -1;
        }

        if (x < 0) {
            return charIndex; // coordinate is outside, to the left of view
        }

        String lineText = mDocument.getLine(line);

        int extent = 0;
        int i = 0;
        while (i < lineText.length()) {
            char c = lineText.charAt(i);
            if (c == ParserDefinition.NEWLINE || c == ParserDefinition.EOF) {
                extent += getEOLAdvance();
            } else if (c == ' ') {
                extent += getSpaceAdvance();
            } else if (c == ParserDefinition.TAB) {
                extent += getTabAdvance();
            } else {
                char[] ca = {c};
                extent += (int) mTextPaint.measureText(ca, 0, 1);
            }

            if (extent >= x) {
                break;
            }

            ++i;
        }

        if (i < lineText.length()) {
            return charIndex + i;
        }

        //nearest char is last char of line
        return charIndex + i - 1;
    }

    public int getAdvance(char c) {
        // Find in cache first
        if (mAdvances[c] != -1) {
            return mAdvances[c];
        }

        int advance;
        switch (c) {
            case ' ':
                advance = getSpaceAdvance();
                break;
            case ParserDefinition.NEWLINE:
            case ParserDefinition.EOF:
                advance = getEOLAdvance();
                break;
            case ParserDefinition.TAB:
                advance = getTabAdvance();
                break;
            default:
                char[] ca = {c};
                advance = (int) mTextPaint.measureText(ca, 0, 1);
                break;
        }
        mAdvances[c] = advance;
        return advance;
    }

    /**
     * @param x 横坐标
     * @param y 纵坐标
     * @return 坐标对应的字符串
     */
    public int coordinateToCharIndexStrict(int x, int y) {
        int line = y / lineHeight();
        int charIndex = mDocument.getLineOffset(line);

        if (charIndex < 0 || x < 0) {
            //non-existent row
            return -1;
        }

        String lineText = mDocument.getLine(line);

        int extent = 0;
        int i = 0;
        while (i < lineText.length()) {
            char c = lineText.charAt(i);
            if (c == ParserDefinition.NEWLINE || c == ParserDefinition.EOF) {
                extent += getEOLAdvance();
            } else if (c == ' ') {
                extent += getSpaceAdvance();
            } else if (c == ParserDefinition.TAB) {
                extent += getTabAdvance();
            } else {
                char[] ca = {c};
                extent += (int) mTextPaint.measureText(ca, 0, 1);
            }

            if (extent >= x) {
                break;
            }

            ++i;
        }

        if (i < lineText.length()) {
            return charIndex + i;
        }

        return -1;
    }

    /**
     * @return X轴滑动极限
     */
    int getMaxScrollX() {
        return (int) Math.max(0,
                mMaxTextWidth - getContentWidth() + mTouchNavigationMethod.getCursorBloat().right + FREE_SCROLL_SPACE_X);
    }

    /**
     * @return Y轴滑动极限
     */
    int getMaxScrollY() {
        return (int) (Math.max(0,
                mDocument.getLineCount() * lineHeight() - getContentHeight() + mTouchNavigationMethod.getCursorBloat().bottom) + FREE_SCROLL_SPACE_Y);
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return getScrollY();
    }

    @Override
    protected int computeVerticalScrollRange() {
        return mDocument.getLineCount() * lineHeight() + getPaddingTop() + getPaddingBottom();
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
        }
    }

    /**
     * 以指定加速度开始滑动
     */
    void flingScroll(int velocityX, int velocityY) {
        mScroller.fling(getScrollX(), getScrollY(), velocityX, velocityY,
                0, getMaxScrollX(), 0, getMaxScrollY());
        postInvalidate();
    }

    /**
     * @return 是否正在滑动
     */
    public boolean isFlingScrolling() {
        return !mScroller.isFinished();
    }

    /**
     * 如果正在滑动,强制停止滑动
     */
    public void stopFlingScrolling() {
        mScroller.forceFinished(true);
    }

    public boolean autoScrollCursor(ScrollTarget scrollDir) {
        boolean scrolled = false;
        switch (scrollDir) {
            case SCROLL_UP:
                if ((!CursorOnFirstLineOfFile())) {
                    moveCursorUp();
                    scrolled = true;
                }
                break;
            case SCROLL_DOWN:
                if (!CursorOnLastLineOfFile()) {
                    moveCursorDown();
                    scrolled = true;
                }
                break;
            case SCROLL_LEFT:
                if (mCursorPosition > 0 &&
                        mCursorLine == mDocument.findLineNumber(mCursorPosition - 1)) {
                    moveCursorLeft();
                    scrolled = true;
                }
                break;
            case SCROLL_RIGHT:
                if (!CursorOnEOF() &&
                        mCursorLine == mDocument.findLineNumber(mCursorPosition + 1)) {
                    moveCursorRight();
                    scrolled = true;
                }
                break;
            default:
                break;
        }
        return scrolled;
    }


    //---------------------------------------------------------------------
    //------------------------- Cursor methods -----------------------------

    public int getCursorLine() {
        return mCursorLine;
    }

    public int getCursorPosition() {
        return mCursorPosition;
    }

    /**
     * Sets the Cursor to position i, scrolls it to view and invalidates
     * the necessary areas for redrawing
     *
     * @param pos The character index that the Cursor should be set to
     */
    public void moveCursor(int pos) {
        if (pos > mDocument.docLength()) {
            pos = mDocument.docLength() - 1;
        }
        mShowCursor = true;
        boolean change = pos != mCursorPosition;
        mEditBehaviorController.moveCursor(pos);
        if (change && mOnEditActionListener != null) {
            mOnEditActionListener.onUpdateCursor();
        }
    }

    /**
     * Sets the Cursor one position back, scrolls it on screen, and invalidates
     * the necessary areas for redrawing.
     * <p/>
     * If the Cursor is already on the first character, nothing will happen.
     */
    public void moveCursorLeft() {
        mEditBehaviorController.moveCursorLeft(false);
    }

    /**
     * Sets the Cursor one position forward, scrolls it on screen, and
     * invalidates the necessary areas for redrawing.
     * <p/>
     * If the Cursor is already on the last character, nothing will happen.
     */
    public void moveCursorRight() {
        mEditBehaviorController.moveCursorRight(false);
    }

    /**
     * Sets the Cursor one row down, scrolls it on screen, and invalidates the
     * necessary areas for redrawing.
     * <p/>
     * If the Cursor is already on the last row, nothing will happen.
     */
    public void moveCursorDown() {
        mEditBehaviorController.moveCursorDown();
    }

    /**
     * Sets the Cursor one row up, scrolls it on screen, and invalidates the
     * necessary areas for redrawing.
     * <p/>
     * If the Cursor is already on the first row, nothing will happen.
     */
    public void moveCursorUp() {
        mEditBehaviorController.moveCursorUp();
    }

    /**
     * Scrolls the Cursor into view if it is not on screen
     */
    public void focusCursor() {
        makeCharVisible(mCursorPosition);
    }

    /**
     * @return 字符偏移所在的列
     */
    protected int getColumn(int charOffset) {
        int line = mDocument.findLineNumber(charOffset);
        int firstCharOfRow = mDocument.getLineOffset(line);
        return charOffset - firstCharOfRow;
    }

    protected boolean CursorOnFirstLineOfFile() {
        return (mCursorLine == 0);
    }

    protected boolean CursorOnLastLineOfFile() {
        return (mCursorLine == (mDocument.getLineCount() - 1));
    }

    protected boolean CursorOnEOF() {
        return (mCursorPosition == (mDocument.docLength() - 1));
    }


    //---------------------------------------------------------------------
    //------------------------- Text Selection ----------------------------

    public final boolean isSelectText() {
        return mEditBehaviorController.isSelectText();
    }

    /**
     * Enter or exit select mode.
     * Invalidates necessary areas for repainting.
     *
     * @param mode If true, enter select mode; else exit select mode
     */
    public void selectText(boolean mode) {
        if (mEditBehaviorController.isSelectText() && !mode) {
            invalidateSelectionLines();
            mEditBehaviorController.setSelectText(false);
        } else if (!mEditBehaviorController.isSelectText() && mode) {
            invalidateCursorLine();
            mEditBehaviorController.setSelectText(true);
        }
    }

    public void selectAll() {
        mEditBehaviorController.setSelectionRange(0, mDocument.docLength() - 1, false);
    }

    public void setSelectionRange(int beginPosition, int numChars) {
        mEditBehaviorController.setSelectionRange(beginPosition, numChars, true);
    }

    public boolean inSelectionRange(int charOffset) {
        return mEditBehaviorController.inSelectionRange(charOffset);
    }

    public int getSelectionStart() {
        return mSelectionLeft;
    }

    public int getSelectionEnd() {
        return mSelectionRight;
    }

    public void focusSelectionStart() {
        mEditBehaviorController.focusSelection(true);
    }

    public void focusSelectionEnd() {
        mEditBehaviorController.focusSelection(false);
    }

    public void cut(ClipboardManager cb) {
        mEditBehaviorController.cut(cb);
    }

    public void copy(ClipboardManager cb) {
        mEditBehaviorController.copy(cb);
    }

    public void paste(String text) {
        mEditBehaviorController.paste(text);
        if (mOnEditActionListener != null) {
            mOnEditActionListener.onPaste(text);
        }
    }

    public Paint getTextPaint() {
        return mTextPaint;
    }

    public void cancelSpanning() {
        mEditBehaviorController.cancelSpanning();
    }

    //---------------------------------------------------------------------
    //------------------------- Formatting methods ------------------------

    /**
     * 设置字体
     */
    public void setTypeface(Typeface typeface) {
        Arrays.fill(this.mAdvances, -1);
        mTextPaint.setTypeface(typeface);
        mEditBehaviorController.updateCursorLine();
        if (!makeCharVisible(mCursorPosition)) {
            invalidate();
        }
    }

    /**
     * 从开始位置删除指定数量的字符
     *
     * @param start 开始地点
     * @param count 删除字符的数量
     */
    public void delete(int start, int count) {
        mDocument.deleteAt(start, count, System.nanoTime());
        updateLeftPadding();
        refreshSpans();
    }

    public boolean isEditable() {
        return isEditable;
    }

    public void setToViewMode() {
        isEditable = false;
    }

    public void setToEditMode() {
        isEditable = true;
    }

    /**
     * 设置字体大小
     */
    public void setZoom(float factor) {
        if (factor <= 0) {
            return;
        }
        Arrays.fill(this.mAdvances, -1);
        this.mMaxTextWidth = 0;
        this.mTextPaint.setTextSize(factor * getContext().getResources().getDisplayMetrics().density);

        updateLeftPadding();
        mEditBehaviorController.updateCursorLine();
        if (!makeCharVisible(mCursorPosition)) {
            invalidate();
        }
    }

    /**
     * 设置Tab所占空格数
     *
     * @param spaceCount Tab所占空格数
     */
    public void setTabSpaces(int spaceCount) {
        if (spaceCount < 0) {
            return;
        }
        mTabLength = spaceCount;
        mEditBehaviorController.updateCursorLine();
        mTabSpaceContent = makeTabs();
        if (!makeCharVisible(mCursorPosition)) {
            invalidate();
        }
    }

    /**
     * 开启/关闭 自动缩进
     */
    public void setAutoIndent(boolean enable) {
        isAutoIndent = enable;
    }

    /**
     * 设置是否高亮当前所在行
     */
    public void setHighlightCurrentLine(boolean enable) {
        isHighlightCurrentLine = enable;
        invalidateCursorLine();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mTouchNavigationMethod.onKeyDown(keyCode, event)) {
            return true;
        }

        if (KeysInterpreter.isNavigationKey(event)) {
            handleNavigationKey(keyCode, event);
            return true;
        }


        char c = KeysInterpreter.keyEventToPrintableChar(event);
        if (c == ParserDefinition.NULL_CHAR) {
            return super.onKeyDown(keyCode, event);
        }

        mEditBehaviorController.onPrintableChar(c);
        if (c != '\b' && onAutoCompletionListener != null) {
            onAutoCompletionListener.onPopCodeComplete(new String(new char[]{c}));
        }

        return true;
    }

    private void handleNavigationKey(int keyCode, KeyEvent event) {
        if (event.isShiftPressed() && !isSelectText()) {
            invalidateCursorLine();
            mEditBehaviorController.setSelectText(true);
        } else if (!event.isShiftPressed() && isSelectText()) {
            invalidateSelectionLines();
            mEditBehaviorController.setSelectText(false);
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                mEditBehaviorController.moveCursorRight(false);
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                mEditBehaviorController.moveCursorLeft(false);
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                mEditBehaviorController.moveCursorDown();
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                mEditBehaviorController.moveCursorUp();
                break;
            default:
                break;
        }
    }

    public OnEditActionListener getOnEditActionListener() {
        return mOnEditActionListener;
    }

    public void setOnEditActionListener(OnEditActionListener onEditActionListener) {
        this.mOnEditActionListener = onEditActionListener;
        mDocument.setOnEditActionListener(onEditActionListener);
    }

    public void updateLeftPadding() {
        mLeftPadding = (int) mTextPaint.measureText(String.valueOf(mDocument.getLineCount() + " "));
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return mTouchNavigationMethod.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isFocused()) {
            mTouchNavigationMethod.onTouchEvent(event);
        } else {
            if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP
                    && isPointInView((int) event.getX(), (int) event.getY())) {
                requestFocus();
            }
        }
        return true;
    }

    /**
     * 点击的位置是否在编辑器中?
     *
     * @param x X坐标
     * @param y Y坐标
     * @return 点击的位置是否在编辑器中
     */
    public boolean isPointInView(int x, int y) {
        return (x >= 0 && x < getWidth() &&
                y >= 0 && y < getHeight());
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        invalidateCursorLine();
    }

    /**
     * Not public to allow access by {@link TouchNavigationMethod}
     */
    public void showInputMethod(boolean show) {
        InputMethodManager im = (InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        // Can we access InputMethodManager?
        if (im == null) {
            return;
        }

        if (!isEditable) {
            im.hideSoftInputFromWindow(this.getWindowToken(), 0);
            return;
        }

        if (show) {
            im.showSoftInput(this, 0);
        } else {
            im.hideSoftInputFromWindow(this.getWindowToken(), 0);
        }
    }

    /**
     * @return 当前的UI状态
     */
    public Parcelable getUiState() {
        return new SuperEditorUIState(this);
    }

    /**
     * 还原UI状态
     *
     * @param state 要还原的UI状态
     */
    public void restoreUiState(Parcelable state) {
        SuperEditorUIState uiState = (SuperEditorUIState) state;
        final int CursorPosition = uiState.cursorPosition;
        if (uiState.selectMode) {
            final int selStart = uiState.selectBegin;
            final int selEnd = uiState.selectEnd;

            post(() -> {
                setSelectionRange(selStart, selEnd - selStart);
                if (CursorPosition < selEnd) {
                    focusSelectionStart();
                }
            });
        } else {
            post(() -> moveCursor(CursorPosition));
        }
    }

    public void refreshSpans() {
        mEditBehaviorController.refreshSpans();
    }

    /**
     * @return 当前文本内容
     */
    public String getText() {
        return new String(mDocument.subSequence(0, mDocument.docLength() - 1));
    }

    /**
     * 设置文本内容
     *
     * @param text 文本内容
     */
    public void setText(String text) {
        Document document = new Document();
        document.insert(text.toCharArray(), 0, 0, false);
        DocumentProvider documentProvider = new DocumentProvider(document);
        setDocumentProvider(documentProvider);
        moveCursor(0);
        refreshSpans();
        updateLeftPadding();
    }

    public void append(String text) {
        paste(text);
    }

    /**
     * 设置语言
     *
     * @param language 语言
     */
    public void setLanguage(Language language) {
        this.mLanguage = language;
        mEditBehaviorController.lexer.setTokenizeAdapter(language.getParserDefinition().getTokenizeAdapter());
    }

    /**
     * 光标滑动标识
     */
    enum ScrollTarget {
        SCROLL_UP,
        SCROLL_DOWN,
        SCROLL_LEFT,
        SCROLL_RIGHT
    }


    OnAutoCompletionListener onAutoCompletionListener;

    public OnAutoCompletionListener getOnAutoCompletionListener() {
        return onAutoCompletionListener;
    }

    public void setOnAutoCompletionListener(OnAutoCompletionListener onAutoCompletionListener) {
        this.onAutoCompletionListener = onAutoCompletionListener;
    }

    public boolean isCursorVisible() {
        return mCursorVisible;
    }

    public void setCursorVisible(boolean cursorVisible) {
        this.mCursorVisible = cursorVisible;
    }

    /**
     * 一个光标循环线程
     */
    static class CursorLooperThread extends Thread {
        WeakReference<XartEditor> ref;

        CursorLooperThread(WeakReference<XartEditor> ref) {
            this.ref = ref;
        }

        @Override
        public void run() {
            super.run();
            for (; ; ) {
                XartEditor xartEditor = ref.get();
                if (xartEditor == null) {
                    break;
                }
                xartEditor.mShowCursor = xartEditor.mCursorVisible && !xartEditor.isSelectText() && !xartEditor.mShowCursor && xartEditor.isFocused();
                xartEditor.post(() -> {
                    XartEditor textField = ref.get();
                    if (!textField.isFlingScrolling()) {
                        textField.invalidateCursorLine();
                    }
                    //不要去掉赋值null,否则内存泄露
                    //noinspection UnusedAssignment
                    textField = null;
                });
                //不要去掉赋值null,否则内存泄露
                //noinspection UnusedAssignment
                xartEditor = null;
                try {
                    Thread.sleep(CURSOR_JUMP_TIME);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    //*********************************************************************
    //************************ 控制器逻辑 ***************************
    //*********************************************************************

    //*********************************************************************
    //**************** 保存和还原UI状态 ******************
    //*********************************************************************

    public static class SuperEditorUIState implements Parcelable {
        public static final Creator<SuperEditorUIState> CREATOR
                = new Creator<SuperEditorUIState>() {
            @Override
            public SuperEditorUIState createFromParcel(Parcel in) {
                return new SuperEditorUIState(in);
            }

            @Override
            public SuperEditorUIState[] newArray(int size) {
                return new SuperEditorUIState[size];
            }
        };
        final int cursorPosition;
        final int scrollX;
        final int scrollY;
        final boolean selectMode;
        final int selectBegin;
        final int selectEnd;

        SuperEditorUIState(XartEditor xartEditor) {
            cursorPosition = xartEditor.getCursorPosition();
            scrollX = xartEditor.getScrollX();
            scrollY = xartEditor.getScrollY();
            selectMode = xartEditor.isSelectText();
            selectBegin = xartEditor.getSelectionStart();
            selectEnd = xartEditor.getSelectionEnd();
        }

        private SuperEditorUIState(Parcel in) {
            cursorPosition = in.readInt();
            scrollX = in.readInt();
            scrollY = in.readInt();
            selectMode = in.readInt() != 0;
            selectBegin = in.readInt();
            selectEnd = in.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(cursorPosition);
            out.writeInt(scrollX);
            out.writeInt(scrollY);
            out.writeInt(selectMode ? 1 : 0);
            out.writeInt(selectBegin);
            out.writeInt(selectEnd);
        }

    }

    private class EditBehaviorController implements Lexer.LexCallback {
        /**
         * 词法分析器
         */
        private Lexer lexer = new Lexer(this);

        {
            lexer.setTokenizeAdapter(mLanguage.getParserDefinition().getTokenizeAdapter());
        }

        private boolean isInSelectionMode = false;

        /**
         * Analyze the text for programming language keywords and redraws the
         * text view when done. The global programming language used is set with
         * the static method Lexer.setLanguage(Language)
         * <p/>
         * Does nothing if the Lexer language is not a programming language
         */
        void refreshSpans() {
            lexer.tokenize(mDocument);
        }

        void cancelSpanning() {
            lexer.cancelTokenize();
        }

        @Override
        public void lexDone(final List<HighlightSpan> results) {
            post(() -> {
                mDocument.setSpans(results);
                invalidate();
            });
        }


        void onPrintableChar(char c) {
            // delete currently selected text, if any
            boolean selectionDeleted = false;
            if (isInSelectionMode) {
                selectionDelete();
                selectionDeleted = true;
            }

            int originalLine = mCursorLine;

            switch (c) {
                case ParserDefinition.BACKSPACE:
                    if (selectionDeleted) {
                        break;
                    }

                    if (mCursorPosition > 0) {
                        mDocument.deleteAt(mCursorPosition - 1, System.nanoTime());
                        moveCursorLeft(true);

                        if (mCursorLine < originalLine) {
                            invalidateFromLine(mCursorLine);
                        }
                    }
                    break;

                case ParserDefinition.NEWLINE:
                    updateLeftPadding();
                    if (isAutoIndent) {
                        char[] indent = createAutoIndent();
                        mDocument.insertBefore(indent, mCursorPosition, System.nanoTime());
                        moveCursor(mCursorPosition + indent.length);
                    } else {
                        mDocument.insertBefore(c, mCursorPosition, System.nanoTime());
                        moveCursorRight(true);
                    }

                    invalidateFromLine(originalLine);
                    break;

                default:
                    mDocument.insertBefore(c, mCursorPosition, System.nanoTime());
                    moveCursorRight(true);

                    break;
            }
            refreshSpans();
            if (mOnEditActionListener != null) {
                mOnEditActionListener.onUpdateCursor();
            }
        }


        /**
         * Return a char[] with a newline as the 0th element followed by the
         * leading spaces and tabs of the line that the Cursor is on
         */
        private char[] createAutoIndent() {
            int lineNum = mDocument.findLineNumber(mCursorPosition);
            int startOfLine = mDocument.getLineOffset(lineNum);
            int whitespaceCount = 0;
            mDocument.seekChar(startOfLine);
            while (mDocument.hasNext()) {
                char c = mDocument.next();
                if (c != ' ' && c != ParserDefinition.TAB) {
                    break;
                }
                ++whitespaceCount;
            }
            char[] indent = new char[1 + whitespaceCount];
            indent[0] = ParserDefinition.NEWLINE;

            mDocument.seekChar(startOfLine);
            for (int i = 0; i < whitespaceCount; ++i) {
                indent[1 + i] = mDocument.next();
            }
            return indent;
        }

        void moveCursorDown() {
            if (!CursorOnLastLineOfFile()) {
                int currCursor = mCursorPosition;
                int currLine = mCursorLine;
                int newLine = currLine + 1;
                int currColumn = getColumn(currCursor);
                int currRowLength = mDocument.getLineSize(currLine);
                int newRowLength = mDocument.getLineSize(newLine);

                if (currColumn < newRowLength) {
                    mCursorPosition += currRowLength;
                } else {
                    mCursorPosition +=
                            currRowLength - currColumn + newRowLength - 1;
                }
                ++mCursorLine;

                updateSelectionRange(currCursor, mCursorPosition);
                if (!makeCharVisible(mCursorPosition)) {
                    invalidateLines(currLine, newLine + 1);
                }
                mLineChangeListener.onLineChange(newLine);
                stopTextComposing();
            }
        }

        void moveCursorUp() {
            if (!CursorOnFirstLineOfFile()) {
                int currCursor = mCursorPosition;
                int currRow = mCursorLine;
                int newRow = currRow - 1;
                int currColumn = getColumn(currCursor);
                int newRowLength = mDocument.getLineSize(newRow);

                if (currColumn < newRowLength) {
                    mCursorPosition -= newRowLength;
                } else {
                    mCursorPosition -= (currColumn + 1);
                }
                --mCursorLine;

                updateSelectionRange(currCursor, mCursorPosition);
                if (!makeCharVisible(mCursorPosition)) {
                    invalidateLines(newRow, currRow + 1);
                }
                mLineChangeListener.onLineChange(newRow);
                stopTextComposing();
            }
        }

        /**
         * @param isTyping Whether Cursor is moved to a consecutive position as
         *                 a result of entering text
         */
        void moveCursorRight(boolean isTyping) {
            if (!CursorOnEOF()) {
                int originalRow = mCursorLine;
                ++mCursorPosition;
                updateCursorLine();
                updateSelectionRange(mCursorPosition - 1, mCursorPosition);
                if (!makeCharVisible(mCursorPosition)) {
                    invalidateLines(originalRow, mCursorLine + 1);
                }

                if (!isTyping) {
                    stopTextComposing();
                }
            }
        }

        /**
         * @param isTyping Whether Cursor is moved to a consecutive position as
         *                 a result of deleting text
         */
        void moveCursorLeft(boolean isTyping) {
            if (mCursorPosition > 0) {
                int originalRow = mCursorLine;
                --mCursorPosition;
                updateCursorLine();
                updateSelectionRange(mCursorPosition + 1, mCursorPosition);
                if (!makeCharVisible(mCursorPosition)) {
                    invalidateLines(mCursorLine, originalRow + 1);
                }

                if (!isTyping) {
                    stopTextComposing();
                }
            }
        }

        void moveCursor(int i) {
            if (i < 0 || i >= mDocument.docLength()) {
                return;
            }

            updateSelectionRange(mCursorPosition, i);
            mCursorPosition = i;
            updateAfterCursorJump();
        }

        private void updateAfterCursorJump() {
            int oldRow = mCursorLine;
            updateCursorLine();
            if (!makeCharVisible(mCursorPosition)) {
                invalidateLines(oldRow, oldRow + 1); //old Cursor row
                invalidateCursorLine(); //new Cursor row
            }
            stopTextComposing();
        }


        /**
         * This helper method should only be used by internal methods after setting
         * _CursorPosition, in order to to recalculate the new row the Cursor is on.
         */
        void updateCursorLine() {
            int newRow = mDocument.findLineNumber(mCursorPosition);
            if (mCursorLine != newRow) {
                mCursorLine = newRow;
                mLineChangeListener.onLineChange(newRow);
            }
        }

        void stopTextComposing() {
            InputMethodManager im = (InputMethodManager) getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (im != null) {
                im.restartInput(XartEditor.this);
            }

            if (mInputConnection != null && mInputConnection.isComposingStarted()) {
                mInputConnection.resetComposingState();
            }
        }

        //- TextFieldController -----------------------------------------------

        /**
         * @return 是否为选择模式
         */
        final boolean isSelectText() {
            return isInSelectionMode;
        }

        /**
         * 进入或退出选择模式
         *
         * @param mode 是否进入选择模式
         */
        void setSelectText(boolean mode) {
            if (mode == isInSelectionMode) {
                return;
            }

            if (mode) {
                mSelectionLeft = mCursorPosition;
                mSelectionRight = mCursorPosition;
            } else {
                mSelectionLeft = -1;
                mSelectionRight = -1;
            }
            isInSelectionMode = mode;
            mSelectionModeChangeListener.onSelectionModeChanged(mode);
        }


        /**
         * 字符偏移是否在选择范围内
         *
         * @param charOffset 字符偏移
         * @return 是否在选择范围内
         */
        boolean inSelectionRange(int charOffset) {
            return isSelectText() && mSelectionLeft >= 0 && (mSelectionLeft <= charOffset && charOffset < mSelectionRight);

        }

        /**
         * Selects numChars count of characters starting from beginPosition.
         * Invalidates necessary areas.
         *
         * @param beginPosition Selection start
         * @param numChars Num chars to select
         * @param scrollToStart If true, the start of the selection will be scrolled
         *                      into view. Otherwise, the end of the selection will be scrolled.
         */
        void setSelectionRange(int beginPosition, int numChars,
                               boolean scrollToStart) {

            if (beginPosition < 0 || beginPosition > mDocument.docLength() || beginPosition + numChars > mDocument.docLength()) {
                return;
            }

            if (isInSelectionMode) {
                invalidateSelectionLines();
            } else {
                invalidateCursorLine();
                setSelectText(true);
            }

            mSelectionLeft = beginPosition;
            mSelectionRight = mSelectionLeft + numChars;

            mCursorPosition = mSelectionRight;
            stopTextComposing();
            updateCursorLine();

            boolean scrolled = makeCharVisible(mSelectionRight);

            if (scrollToStart) {
                scrolled = makeCharVisible(mSelectionLeft);
            }


            if (!scrolled) {
                invalidateSelectionLines();
            }
        }

        /**
         * Moves the Cursor to an edge of selected text and scrolls it to view.
         *
         * @param start If true, moves the Cursor to the beginning of
         *              the selection. Otherwise, moves the Cursor to the end of the selection.
         *              In all cases, the Cursor is scrolled to view if it is not visible.
         */
        void focusSelection(boolean start) {
            if (isInSelectionMode) {
                if (start && mCursorPosition != mSelectionLeft) {
                    mCursorPosition = mSelectionLeft;
                    updateAfterCursorJump();
                } else if (!start && mCursorPosition != mSelectionRight) {
                    mCursorPosition = mSelectionRight;
                    updateAfterCursorJump();
                }
            }
        }


        /**
         * Used by internal methods to update selection boundaries when a new
         * Cursor position is set.
         * Does nothing if not in selection mode.
         */
        private void updateSelectionRange(int oldCursorPosition, int newCursorPosition) {
            if (!isInSelectionMode) {
                return;
            }

            if (oldCursorPosition < mSelectionRight) {
                if (newCursorPosition > mSelectionRight) {
                    mSelectionLeft = mSelectionRight;
                    mSelectionRight = newCursorPosition;
                } else {
                    mSelectionLeft = newCursorPosition;
                }

            } else {
                if (newCursorPosition < mSelectionLeft) {
                    mSelectionRight = mSelectionLeft;
                    mSelectionLeft = newCursorPosition;
                } else {
                    mSelectionRight = newCursorPosition;
                }
            }
        }


        //- TextFieldController -----------------------------------------------
        //------------------------ Cut, copy, paste ---------------------------

        /**
         * Convenience method for consecutive copy and paste calls
         */
        void cut(ClipboardManager cb) {
            copy(cb);
            selectionDelete();
        }

        /**
         * Copies the selected text to the clipboard.
         * <p/>
         * Does nothing if not in select mode.
         */
        public void copy(ClipboardManager cb) {
            if (isInSelectionMode &&
                    mSelectionLeft < mSelectionRight) {
                try {
                    char[] contents = mDocument.subSequence(mSelectionLeft,
                            mSelectionRight - mSelectionLeft);
                    cb.setPrimaryClip(ClipData.newPlainText(null, new String(contents)));
                } catch (OutOfMemoryError error) {
                    IEventManager eventManager = ServiceManager.get().getService(IEventManager.class);
                    eventManager.sendEvent(new InternalErrorEvent(error));
                }
            }
        }

        /**
         * Inserts text at the Cursor position.
         * Existing selected text will be deleted and select mode will end.
         * The deleted area will be invalidated.
         * <p/>
         * After insertion, the inserted area will be invalidated.
         */
        void paste(String text) {
            if (text == null) {
                return;
            }

            mDocument.beginBatchEdit();
            selectionDelete();

            int originalRow = mCursorLine;
            mDocument.insertBefore(text.toCharArray(), mCursorPosition, System.nanoTime());
            mDocument.endBatchEdit();

            mCursorPosition += text.length();
            updateCursorLine();
            refreshSpans();
            stopTextComposing();

            if (!makeCharVisible(mCursorPosition)) {
                //invalidate previous row too if its wrapping changed

                if (originalRow == mCursorLine) {
                    //pasted text only affects Cursor row
                    invalidateLines(originalRow, originalRow + 1);
                } else {
                    invalidateFromLine(originalRow);
                }
            }
            updateLeftPadding();
        }

        /**
         * Deletes selected text, exits select mode and invalidates deleted area.
         * If the selected range is empty, this method exits select mode and
         * invalidates the Cursor.
         * <p/>
         * Does nothing if not in select mode.
         */
        void selectionDelete() {
            if (!isInSelectionMode) {
                return;
            }

            int totalChars = mSelectionRight - mSelectionLeft;

            if (totalChars > 0) {
                int originalRow = mDocument.findLineNumber(mSelectionLeft);
                boolean isSingleRowSel = mDocument.findLineNumber(mSelectionRight) == originalRow;
                mDocument.deleteAt(mSelectionLeft, totalChars, System.nanoTime());

                mCursorPosition = mSelectionLeft;
                updateCursorLine();
                refreshSpans();
                setSelectText(false);
                stopTextComposing();

                if (!makeCharVisible(mCursorPosition)) {
                    //invalidate previous row too if its wrapping changed

                    if (isSingleRowSel) {
                        //pasted text only affects current row
                        invalidateLines(originalRow, originalRow + 1);
                    } else {
                        //TODO invalidate damaged rows only
                        invalidateFromLine(originalRow);
                    }
                }
            } else {
                setSelectText(false);
                invalidateCursorLine();
            }
        }

        void replaceComposingText(int from, int charCount, String text) {
            int invalidateStartRow;
            boolean isInvalidateSingleRow = true;
            boolean dirty = false;

            //delete selection
            if (isInSelectionMode) {
                invalidateStartRow = mDocument.findLineNumber(mSelectionLeft);

                int totalChars = mSelectionRight - mSelectionLeft;

                if (totalChars > 0) {
                    mCursorPosition = mSelectionLeft;
                    mDocument.deleteAt(mSelectionLeft, totalChars, System.nanoTime());

                    if (invalidateStartRow != mCursorLine) {
                        isInvalidateSingleRow = false;
                    }
                    dirty = true;
                }

                setSelectText(false);
            } else {
                invalidateStartRow = mCursorLine;
            }

            if (charCount > 0) {
                int delFromRow = mDocument.findLineNumber(from);
                if (delFromRow < invalidateStartRow) {
                    invalidateStartRow = delFromRow;
                }

                if (invalidateStartRow != mCursorLine) {
                    isInvalidateSingleRow = false;
                }


                mCursorPosition = from;
                mDocument.deleteAt(from, charCount, System.nanoTime());
                dirty = true;
            }

            if (text != null && text.length() > 0) {
                int insFromRow = mDocument.findLineNumber(from);
                if (insFromRow < invalidateStartRow) {
                    invalidateStartRow = insFromRow;
                }

                mDocument.insertBefore(text.toCharArray(), mCursorPosition, System.nanoTime());
                mCursorPosition += text.length();
                dirty = true;
            }

            if (dirty) {
                refreshSpans();
            }

            int originalRow = mCursorLine;
            updateCursorLine();
            if (originalRow != mCursorLine) {
                isInvalidateSingleRow = false;
            }

            if (!makeCharVisible(mCursorPosition)) {
                if (isInvalidateSingleRow) {
                    invalidateLines(mCursorLine, mCursorLine + 1);
                } else {
                    //TODO invalidate damaged rows only
                    invalidateFromLine(invalidateStartRow);
                }
            }
            if (mOnEditActionListener != null) {
                mOnEditActionListener.onUpdateCursor();
            }
        }

        void deleteAroundComposingText(int left, int right) {
            int start = mCursorPosition - left;
            if (start < 0) {
                start = 0;
            }
            int end = mCursorPosition + right;
            int docLength = mDocument.docLength();
            if (end > (docLength - 1)) {
                end = docLength - 1;
            }
            replaceComposingText(start, end - start, "");
        }

        String getTextAfterCursor(int maxLen) {
            int docLength = mDocument.docLength();
            if ((mCursorPosition + maxLen) > (docLength - 1)) {
                return new String(
                        mDocument.subSequence(mCursorPosition, docLength - mCursorPosition - 1));
            }

            return new String(mDocument.subSequence(mCursorPosition, maxLen));
        }

        String getTextBeforeCursor(int maxLen) {
            int start = mCursorPosition - maxLen;
            if (start < 0) {
                start = 0;
            }
            return new String(mDocument.subSequence(start, mCursorPosition - start));
        }
    }

    //*********************************************************************
    //************************** InputConnection **************************
    //*********************************************************************
    private class TextFieldInputConnection extends BaseInputConnection {
        private boolean _isComposing = false;
        private int _composingCharCount = 0;

        TextFieldInputConnection(XartEditor v) {
            super(v, true);
        }

        void resetComposingState() {
            _composingCharCount = 0;
            _isComposing = false;
            mDocument.endBatchEdit();
        }

        /**
         * Only true when the InputConnection has not been used by the IME yet.
         * Can be programatically cleared by resetComposingState()
         */
        boolean isComposingStarted() {
            return _isComposing;
        }

        @Override
        public boolean setComposingText(CharSequence text, int newCursorPosition) {
            _isComposing = true;
            if (!mDocument.isBatchEdit()) {
                mDocument.beginBatchEdit();
            }

            mEditBehaviorController.replaceComposingText(
                    getCursorPosition() - _composingCharCount,
                    _composingCharCount,
                    text.toString());
            _composingCharCount = text.length();

            if (newCursorPosition > 1) {
                mEditBehaviorController.moveCursor(mCursorPosition + newCursorPosition - 1);
            } else if (newCursorPosition <= 0) {
                mEditBehaviorController.moveCursor(mCursorPosition - text.length() - newCursorPosition);
            }
            return true;
        }

        @Override
        public boolean commitText(CharSequence text, int newCursorPosition) {
            _isComposing = true;
            mEditBehaviorController.replaceComposingText(
                    getCursorPosition() - _composingCharCount,
                    _composingCharCount,
                    text.toString());
            _composingCharCount = 0;
            mDocument.endBatchEdit();

            if (newCursorPosition > 1) {
                mEditBehaviorController.moveCursor(mCursorPosition + newCursorPosition - 1);
            } else if (newCursorPosition <= 0) {
                mEditBehaviorController.moveCursor(mCursorPosition - text.length() - newCursorPosition);
            }
            if (onAutoCompletionListener != null) {
                onAutoCompletionListener.onPopCodeComplete(text);

            }
            return true;
        }


        @Override
        public boolean deleteSurroundingText(int leftLength, int rightLength) {

            mEditBehaviorController.deleteAroundComposingText(leftLength, rightLength);
            return true;
        }

        @Override
        public boolean finishComposingText() {
            resetComposingState();
            return true;
        }

        @Override
        public int getCursorCapsMode(int reqModes) {
            int capsMode = 0;
            ParserDefinition parserDefinition = mLanguage.getParserDefinition();

            if ((reqModes & InputType.TYPE_TEXT_FLAG_CAP_WORDS)
                    == InputType.TYPE_TEXT_FLAG_CAP_WORDS) {
                int prevChar = mCursorPosition - 1;
                if (prevChar < 0 || parserDefinition.isWhitespace(mDocument.charAt(prevChar))) {
                    capsMode |= InputType.TYPE_TEXT_FLAG_CAP_WORDS;

                    if ((reqModes & InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
                            == InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) {
                        capsMode |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
                    }
                }
            } else {

                int prevChar = mCursorPosition - 1;
                int whitespaceCount = 0;
                boolean capsOn = true;
                while (prevChar >= 0) {
                    char c = mDocument.charAt(prevChar);
                    if (c == ParserDefinition.NEWLINE) {
                        break;
                    }

                    if (!parserDefinition.isWhitespace(c)) {
                        if (whitespaceCount == 0 || !parserDefinition.isSentenceTerminator(c)) {
                            capsOn = false;
                        }
                        break;
                    }

                    ++whitespaceCount;
                    --prevChar;
                }

                if (capsOn) {
                    capsMode |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
                }
            }

            return capsMode;
        }

        @Override
        public CharSequence getTextAfterCursor(int maxLen, int flags) {
            return mEditBehaviorController.getTextAfterCursor(maxLen);
        }


        @Override
        public CharSequence getTextBeforeCursor(int maxLen, int flags) {
            return mEditBehaviorController.getTextBeforeCursor(maxLen);
        }

        @Override
        public boolean setSelection(int start, int end) {
            if (start == end) {
                mEditBehaviorController.moveCursor(start);
            } else {
                mEditBehaviorController.setSelectionRange(start, end - start, false);
            }
            return true;
        }

    }


}
