package com.aurora.main.activities

import android.os.Bundle
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.viewpager2.widget.ViewPager2
import com.aurora.R
import com.aurora.main.adapters.MyBaseFragmentAdapter
import com.aurora.main.fragments.HomeFragment
import com.aurora.main.fragments.MenuFragment
import com.aurora.main.fragments.SettingsFragment
import org.aurora.core.BaseCompatActivity
import org.aurora.view.MyBaseToolbar


class MainActivity : BaseCompatActivity() {

    private lateinit var viewpager1: ViewPager2
    private lateinit var my_toolbar: MyBaseToolbar
    private lateinit var compose_nav_bar: ComposeView
    private lateinit var main_frame: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        initialize()
        initializeLogic()
    }

    private fun initialize() {
        viewpager1 = findViewById(R.id.viewpager1)
        my_toolbar = findViewById(R.id.my_toolbar)
        compose_nav_bar = findViewById(R.id.compose_nav_bar)
        main_frame = findViewById(R.id._main)
    }

    private fun initializeLogic() {
        applySystemBarInsets(main_frame, my_toolbar, compose_nav_bar)
        val adapter = MyBaseFragmentAdapter(this)
        
        adapter.addFragment(HomeFragment())
        adapter.addFragment(MenuFragment())
        adapter.addFragment(SettingsFragment())
        
        viewpager1.adapter = adapter
        
        setupComposeBottomNav()
    }
    
    private fun setupComposeBottomNav() {
        compose_nav_bar.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    var selectedIndex by remember { mutableIntStateOf(0) }

                    // Sync ViewPager2 with Compose
                    LaunchedEffect(viewpager1) {
                        viewpager1.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                            override fun onPageSelected(position: Int) {
                                selectedIndex = position
                            }
                        })
                    }

                    CustomBottomNav(
                        selectedIndex = selectedIndex,
                        onItemSelected = { index ->
                            selectedIndex = index
                            viewpager1.setCurrentItem(index, true)
                        }
                    )
                }
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
        NavItem(R.drawable.ic_game, "Game"),
        NavItem(R.drawable.ic_widgets, "Menu"),
        NavItem(R.drawable.ic_settings, "Settings")
    )

    Surface(
        color = Color(0xFF1E2022), // Dark pill background from the image
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
    val backgroundColor = if (isSelected) Color(0xFF1B4958) else Color.Transparent
    val contentColor = if (isSelected) Color.White else Color(0xFF9AA0A6)

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
            contentDescription = item.title,
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
                    text = item.title,
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

data class NavItem(val iconRes: Int, val title: String)
