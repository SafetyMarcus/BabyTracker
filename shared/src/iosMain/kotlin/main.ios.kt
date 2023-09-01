import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import com.seiko.imageloader.ImageLoader
import com.seiko.imageloader.cache.memory.maxSizePercent
import com.seiko.imageloader.component.setupDefaultComponents
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.fromMilliseconds
import dev.gitlive.firebase.firestore.toMilliseconds
import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.cstr
import kotlinx.coroutines.DisposableHandle
import kotlinx.datetime.Instant
import okio.Path.Companion.toPath
import platform.CoreFoundation.kCFAbsoluteTimeIntervalSince1970
import platform.CoreGraphics.CGRectGetMidX
import platform.CoreGraphics.CGRectGetMidY
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterRoundDown
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSTimeIntervalSince1970
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDestructive
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyle
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIButton
import platform.UIKit.UIButtonConfiguration
import platform.UIKit.UIColor
import platform.UIKit.UIControl
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIControlEventValueChanged
import platform.UIKit.UIControlEvents
import platform.UIKit.UIControlStateNormal
import platform.UIKit.UIDatePicker
import platform.UIKit.UIDatePickerMode
import platform.UIKit.UIDatePickerStyle
import platform.UIKit.UIEdgeInsetsMake
import platform.UIKit.UILayoutConstraintAxisVertical
import platform.UIKit.UIModalPresentationPopover
import platform.UIKit.UISheetPresentationControllerDetent
import platform.UIKit.UIStackView
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController
import platform.UIKit.sheetPresentationController
import platform.darwin.NSObject
import platform.objc.OBJC_ASSOCIATION_RETAIN
import platform.objc.objc_removeAssociatedObjects
import platform.objc.objc_setAssociatedObject

actual fun getPlatformName(): String = "iOS"

private fun Timestamp.toNSDate(): NSDate? {
    val isoFormatter = NSDateFormatter()
    isoFormatter.locale = NSLocale("en_US_POSIX")
    isoFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
    val iso = Instant.fromEpochSeconds(this.seconds, this.nanoseconds).toString()
    return isoFormatter.dateFromString(iso)
}
actual fun Timestamp.format(format: String): String {
    val formatter = NSDateFormatter()
    formatter.dateStyle = NSDateFormatterMediumStyle
    formatter.dateFormat = format
    return formatter.stringFromDate(toNSDate() ?: return "")
}

actual fun randomUUID(): String = NSUUID().UUIDString

actual fun generateImageLoader(): ImageLoader = ImageLoader {
    components {
        setupDefaultComponents()
    }
    interceptor {
        memoryCacheConfig {
            maxSizeBytes(32 * 1024 * 1024)
        }
        diskCacheConfig {
            directory(getCacheDir().toPath().resolve("image_cache"))
            maxSizeBytes(512L * 1024 * 1024) // 512MB
        }
    }
}

private fun getCacheDir(): String {
    return NSSearchPathForDirectoriesInDomains(
        NSCachesDirectory,
        NSUserDomainMask,
        true,
    ).first() as String
}

actual fun Float.format() = NSNumberFormatter().apply {
    roundingMode = NSNumberFormatterRoundDown
}.stringFromNumber(NSNumber(this)) ?: ""

@Composable
actual fun DeleteAlert(
    deleteClicked: () -> Unit,
    cancelClicked: () -> Unit,
) {
    val alert = UIAlertController().apply {
        title = "Delete"
        message = "Are you sure you want to delete this event?"
        val sender = LocalUIViewController.current.view
        popoverPresentationController?.setPermittedArrowDirections(0u)
        popoverPresentationController?.sourceView = sender
        popoverPresentationController?.sourceRect = CGRectMake(
            x = CGRectGetMidX(sender.bounds),
            y = CGRectGetMidY(sender.bounds),
            width = 0.0,
            height = 0.0
        )
        addAction(
            UIAlertAction.actionWithTitle(
                title = "Cancel",
                style = UIAlertActionStyleCancel,
                handler = { cancelClicked() }
            )
        )
        addAction(
            UIAlertAction.actionWithTitle(
                title = "Delete",
                style = UIAlertActionStyleDestructive,
                handler = { deleteClicked() }
            )
        )
    }
    LocalUIViewController.current.presentViewController(
        viewControllerToPresent = alert,
        animated = true,
        completion = null
    )
}

@Composable
actual fun TimePickerAlert(
    current: Timestamp,
    onSet: (Timestamp) -> Unit,
    onDismiss: () -> Unit,
) {
    val original by remember {
        mutableStateOf(current.toNSDate())
    }
    var date by remember {
        mutableStateOf(current.toNSDate() ?: NSDate())
    }
    DatePickerViewController(
        selectedDate = original ?: NSDate(),
        onDateChanged = { updated -> date = updated },
        onSave = {
            onSet(Timestamp.fromMilliseconds(date.timeIntervalSince1970 * 1000))
        },
        datePickerCustomizer = {
            setPreferredDatePickerStyle(UIDatePickerStyle.UIDatePickerStyleWheels)
            datePickerMode = UIDatePickerMode.UIDatePickerModeTime
            maximumDate = NSDate() // has to be in past
        },
        onDismissRequest = onDismiss,
    )
}

@Composable
internal fun DatePickerViewController(
    selectedDate: NSDate,
    onDateChanged: (NSDate) -> Unit,
    onSave: () -> Unit,
    onDismissRequest: () -> Unit,
    datePickerCustomizer: UIDatePicker.() -> Unit = {},
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
) {
    val viewController = LocalUIViewController.current

    val lastOnSave by rememberUpdatedState(onSave)
    val lastOnDateChanged by rememberUpdatedState(onDateChanged)
    val lastOnDismissRequest by rememberUpdatedState(onDismissRequest)

    val datePickerViewController = remember {
        DatePickerViewController(backgroundColor).apply {
            datePickerCustomizer(datePicker)

            confirmButton.setTitle("Ok", UIControlStateNormal)
        }
    }

    DisposableEffect(datePickerViewController) {
        val handle = datePickerViewController.datePicker
            .addEventHandler(UIControlEventValueChanged) {
                lastOnDateChanged(date)
            }
        onDispose {
            handle.dispose()
        }
    }

    DisposableEffect(datePickerViewController) {
        val handle = datePickerViewController.confirmButton
            .addEventHandler(UIControlEventTouchUpInside) {
                lastOnSave()
            }
        onDispose {
            handle.dispose()
        }
    }

    LaunchedEffect(datePickerViewController, selectedDate) {
        datePickerViewController.datePicker.setDate(selectedDate)
    }

    DisposableEffect(viewController, datePickerViewController) {
        datePickerViewController.sheetPresentationController?.apply {
            detents = listOf(UISheetPresentationControllerDetent.mediumDetent())
        }

        viewController.presentViewController(datePickerViewController, true, null)

        datePickerViewController.onViewDisappeared = { lastOnDismissRequest() }

        onDispose {
            datePickerViewController.dismissViewControllerAnimated(true) {
                lastOnDismissRequest()
            }
        }
    }
}

private class DatePickerViewController(
    private val backgroundColor: Color,
) : UIViewController(nibName = null, bundle = null) {
    val datePicker = UIDatePicker().apply {
        translatesAutoresizingMaskIntoConstraints = false
    }
    val confirmButton = UIButton().apply {
        configuration = UIButtonConfiguration.borderlessButtonConfiguration()
        translatesAutoresizingMaskIntoConstraints = false
    }

    val stack = UIStackView().apply {
        axis = UILayoutConstraintAxisVertical
        spacing = 16.0
        layoutMarginsRelativeArrangement = true
        layoutMargins = UIEdgeInsetsMake(24.0, 24.0, 24.0, 24.0)
        translatesAutoresizingMaskIntoConstraints = false
    }

    var onViewDisappeared: () -> Unit = {}

    override fun viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = UIColor(
            red = backgroundColor.red.toDouble(),
            green = backgroundColor.green.toDouble(),
            blue = backgroundColor.blue.toDouble(),
            alpha = backgroundColor.alpha.toDouble(),
        )

        view.addSubview(stack)

        NSLayoutConstraint.activateConstraints(
            listOf(
                stack.topAnchor.constraintEqualToAnchor(view.topAnchor),
                stack.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor),
                stack.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor),
            ),
        )

        stack.insertArrangedSubview(datePicker, 0u)
        stack.insertArrangedSubview(confirmButton, 1u)
    }

    override fun viewDidDisappear(animated: Boolean) {
        super.viewDidDisappear(animated)
        onViewDisappeared()
    }
}

fun <T : UIControl> T.addEventHandler(
    event: UIControlEvents,
    lambda: T.() -> Unit,
): DisposableHandle {
    val lambdaTarget = ControlLambdaTarget(lambda)
    val action = NSSelectorFromString("action:")

    addTarget(
        target = lambdaTarget,
        action = action,
        forControlEvents = event,
    )

    objc_setAssociatedObject(
        `object` = this,
        key = "event$event".cstr,
        value = lambdaTarget,
        policy = OBJC_ASSOCIATION_RETAIN,
    )

    return DisposableHandle {
        removeTarget(target = lambdaTarget, action = action, forControlEvents = event)
        objc_removeAssociatedObjects(this@addEventHandler)
    }
}

@ExportObjCClass
private class ControlLambdaTarget<T : UIControl>(
    private val lambda: T.() -> Unit,
) : NSObject() {
    @ObjCAction
    fun action(sender: UIControl) {
        @Suppress("UNCHECKED_CAST")
        lambda(sender as T)
    }
}

fun MainViewController(
    viewModel: MainViewModel,
) = ComposeUIViewController {
    App(viewModel = viewModel)
}