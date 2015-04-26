package ru.yandex.qatools.allure.data.plugins

import groovy.transform.EqualsAndHashCode
import ru.yandex.qatools.allure.data.AllureDefect
import ru.yandex.qatools.allure.data.AllureDefects
import ru.yandex.qatools.allure.data.AllureTestCase
import ru.yandex.qatools.allure.data.DefectItem
import ru.yandex.qatools.allure.data.DefectsWidgetItem
import ru.yandex.qatools.allure.data.utils.PluginUtils
import ru.yandex.qatools.allure.model.Status

import static ru.yandex.qatools.allure.data.utils.TextUtils.generateUid
import static ru.yandex.qatools.allure.data.utils.TextUtils.getMessageMask
import static ru.yandex.qatools.allure.model.Status.BROKEN
import static ru.yandex.qatools.allure.model.Status.FAILED
import static ru.yandex.qatools.allure.model.Status.PASSED

/**
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 06.02.15
 */
@Plugin.Name("defects")
@Plugin.Priority(500)
class DefectsPlugin extends DefaultTabPlugin implements WithWidget {

    public static final int DEFECTS_IN_WIDGET = 10

    @Plugin.Data
    def defects = new AllureDefects(defectsList: [
            new AllureDefect(title: "Product defects", status: FAILED),
            new AllureDefect(title: "Test defects", status: BROKEN)
    ])

    private Map<Key, DefectItem> defectItems = new HashMap<>()

    /**
     * Process given test cases and if it failed or broken add it to defects tab.
     */
    @Override
    void process(AllureTestCase testCase) {
        if (!(testCase.status in [FAILED, BROKEN])) {
            return;
        }
        Key key = new Key(uid: getMessageMask(testCase?.failure?.message), status: testCase.status)
        if (!defectItems.containsKey(key)) {
            def item = new DefectItem(uid: generateUid(), failure: testCase.failure)
            defectItems.put(key, item);
            getDefect(testCase.status)?.defects?.add(item)
        }

        use(PluginUtils) {
            defectItems[key].testCases.add(testCase.toInfo())
        }
    }

    /**
     * Creates a widget for defects. This widget will contains first {@link #DEFECTS_IN_WIDGET}
     * defects from {@link #defects}.
     */
    @Override
    Widget getWidget() {
        def widget = new DefectsWidget(name)
        def failed = getDefect(FAILED).defects.take(DEFECTS_IN_WIDGET)
        def broken = getDefect(BROKEN).defects.take(DEFECTS_IN_WIDGET - failed.size())

        widget.data = []
        widget.data += failed.collect {
            new DefectsWidgetItem(message: it?.failure?.message, status: FAILED, count: it.testCases.size())
        }
        widget.data += broken.collect {
            new DefectsWidgetItem(message: it?.failure?.message, status: BROKEN, count: it.testCases.size())
        }
        if (widget.data.empty) {
            widget.data.add(new DefectsWidgetItem(message: "There are no defects!", status: PASSED))
        }
        widget
    }

    /**
     * Find defect by given status.
     */
    private AllureDefect getDefect(Status status) {
        defects.defectsList.find { it.status == status }
    }

    /**
     * Defect status - uid pair.
     * @see #defects
     */
    @EqualsAndHashCode
    class Key {
        Status status;
        String uid;
    }
}
