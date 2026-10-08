package com.aurora.main.activities

import android.os.Bundle
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.viewpager2.widget.ViewPager2

import com.aurora.R
import com.aurora.main.adapters.MyBaseFragmentAdapter
import com.aurora.main.fragments.HomeFragment
import com.aurora.main.fragments.MenuFragment
import com.aurora.main.fragments.SettingsFragment

import kotlinx.coroutines.flow.MutableStateFlow

import org.aurora.core.BaseCompatActivity

class MainActivity : BaseCompatActivity() {

    private lateinit var viewpager1: ViewPager2
    private lateinit var compose_top_bar: ComposeView
    private lateinit var compose_nav_bar: ComposeView
    private lateinit var main_frame: FrameLayout
    private val _selectedPageIndex = MutableStateFlow(0)
    
    // ambil nama dari file strings.xml
    private val pageTitleResIds = listOf(
        R.string.title_toolbar_name_main_game,
        R.string.title_toolbar_name_main_menu,
        R.string.title_toolbar_name_main_settings
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        initialize()
        initializeLogic()
    }

    private fun initialize() {
        viewpager1 = findViewById(R.id.viewpager1)
        compose_top_bar = findViewById(R.id.compose_top_bar)
        compose_nav_bar = findViewById(R.id.compose_nav_bar)
        main_frame = findViewById(R.id._main)
    }

    private fun initializeLogic() {
        applySystemBarInsets(main_frame, compose_top_bar, compose_nav_bar)
        val adapter = MyBaseFragmentAdapter(this)
        adapter.addFragment(HomeFragment())
        adapter.addFragment(MenuFragment())
        adapter.addFragment(SettingsFragment())
        
        viewpager1.adapter = adapter
        
        viewpager1.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                _selectedPageIndex.value = position
            }
        })
        
        setupComposeTopBar()
        setupComposeBottomNav()
    }
    
    private fun setupComposeTopBar() {
        compose_top_bar.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    val pageIndex by _selectedPageIndex.collectAsState()
                    val currentTitleRes = pageTitleResIds.getOrElse(pageIndex) { R.string.title_toolbar_name_main_game }
                    val currentTitle = stringResource(id = currentTitleRes)
                    
                    CustomTopBar(title = currentTitle)
                }
            }
        }
    }

    private fun setupComposeBottomNav() {
        compose_nav_bar.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    val pageIndex by _selectedPageIndex.collectAsState()

                    CustomBottomNav(
                        selectedIndex = pageIndex,
                        onItemSelected = { index ->
                            viewpager1.setCurrentItem(index, true)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomTopBar(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Animasi transisi teks saat di-swipe
        AnimatedContent(
            targetState = title,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            }, label = "titleAnimation"
        ) { targetTitle ->
            Text(
                text = targetTitle,
                color = colorResource(id = R.color.onSurface), 
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        // Bagian Kanan (Nama & ID Badge)
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.title_developer),
                color = colorResource(id = R.color.color_white_slow),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(
                        color = colorResource(id = R.color.surfaceContainerHigh), 
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "ID 923ba551",
                    color = colorResource(id = R.color.primary), // Menggunakan primary sebagai accent color
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun CustomBottomNav(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    val items = listOf(
        NavItem(R.drawable.ic_game, R.string.title_toolbar_name_main_game),
        NavItem(R.drawable.ic_widgets, R.string.title_toolbar_name_main_menu),
        NavItem(R.drawable.ic_settings, R.string.title_toolbar_name_main_settings)
    )

    Surface(
        color = colorResource(id = R.color.surfaceContainer),
        shape = CircleShape,
        shadowElevation = 8.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                CustomNavItem(
                    item = item,
                    isSelected = selectedIndex == index,
                    onClick = { onItemSelected(index) }
                )
            }
        }
    }
}

@Composable
fun CustomNavItem(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    // Pemetaan warna Material 3 dari colors.xml
    val backgroundColor = if (isSelected) colorResource(id = R.color.secondaryContainer) else Color.Transparent
    val contentColor = if (isSelected) colorResource(id = R.color.onSecondaryContainer) else colorResource(id = R.color.onSurfaceVariant)
    
    val titleText = stringResource(id = item.titleRes)

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = item.iconRes),
            contentDescription = titleText,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        AnimatedVisibility(
            visible = isSelected,
            enter = expandHorizontally(animationSpec = tween(300)),
            exit = shrinkHorizontally(animationSpec = tween(300))
        ) {
            Row {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = titleText,
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

data class NavItem(val iconRes: Int, val titleRes: Int)