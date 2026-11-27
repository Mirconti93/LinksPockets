package it.linkspockets.presentation

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIColor
import platform.UIKit.UIViewController
import platform.UIKit.systemBackgroundColor

/** Empty iOS screen, exposed to the future Swift/SwiftUI host. */
@OptIn(ExperimentalForeignApi::class)
fun MainViewController(): UIViewController = object : UIViewController(nibName = null, bundle = null) {
    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.systemBackgroundColor
    }
}
