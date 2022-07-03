package io.github.kineks.neteaseviewer.ui.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun BottomNavigationBar(
    selectedItem: Int,
    navItemList: List<String>,
    navItemIconList: List<ImageVector>,
    whenIndexChange: (Int) -> Unit,
    backgroundColor: Color = MaterialTheme.colors.background
) {
    BottomNavigation(backgroundColor = backgroundColor) {
        for (index in navItemList.indices) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clickable(
                        onClick = {
                            whenIndexChange(index)
                        }
                    ),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NavigationIcon(
                    index == selectedItem,
                    navItemIconList[index],
                    navItemList[index]
                )
            }
        }
    }
}

@Composable
private fun NavigationIcon(
    selected: Boolean,
    icon: ImageVector,
    title: String
) {

    val alpha = if (selected) 1f else 0.5f
    Icon(icon, contentDescription = title, modifier = Modifier.alpha(alpha))

    Spacer(Modifier.padding(top = 2.dp))
    AnimatedVisibility(visible = selected) {
        Surface(
            shape = CircleShape,
            modifier = Modifier.size(5.dp),
            color = MaterialTheme.colors.onSurface
        ) { }
    }

}